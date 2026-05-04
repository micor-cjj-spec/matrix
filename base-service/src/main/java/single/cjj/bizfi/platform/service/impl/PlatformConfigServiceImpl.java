package single.cjj.bizfi.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.platform.dto.PlatformMenuResponse;
import single.cjj.bizfi.platform.dto.PlatformModuleGroupResponse;
import single.cjj.bizfi.platform.dto.PlatformModuleHubResponse;
import single.cjj.bizfi.platform.dto.PlatformUiItemResponse;
import single.cjj.bizfi.platform.dto.PlatformWorkbenchResponse;
import single.cjj.bizfi.platform.entity.MatrixPlatformApp;
import single.cjj.bizfi.platform.entity.MatrixPlatformMenu;
import single.cjj.bizfi.platform.entity.MatrixPlatformModuleItem;
import single.cjj.bizfi.platform.entity.MatrixPlatformWorkbenchItem;
import single.cjj.bizfi.platform.mapper.MatrixPlatformAppMapper;
import single.cjj.bizfi.platform.mapper.MatrixPlatformMenuMapper;
import single.cjj.bizfi.platform.mapper.MatrixPlatformModuleItemMapper;
import single.cjj.bizfi.platform.mapper.MatrixPlatformWorkbenchItemMapper;
import single.cjj.bizfi.platform.service.PlatformConfigService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PlatformConfigServiceImpl implements PlatformConfigService {

    private static final String STATUS_ENABLED = "ENABLED";

    @Autowired
    private MatrixPlatformAppMapper appMapper;

    @Autowired
    private MatrixPlatformMenuMapper menuMapper;

    @Autowired
    private MatrixPlatformWorkbenchItemMapper workbenchItemMapper;

    @Autowired
    private MatrixPlatformModuleItemMapper moduleItemMapper;

    @Override
    public PlatformWorkbenchResponse getWorkbench() {
        PlatformWorkbenchResponse response = new PlatformWorkbenchResponse();
        response.setApps(getApps());

        Map<String, List<MatrixPlatformWorkbenchItem>> sections = workbenchItemMapper.selectList(
                new LambdaQueryWrapper<MatrixPlatformWorkbenchItem>()
                        .eq(MatrixPlatformWorkbenchItem::getFstatus, STATUS_ENABLED)
                        .orderByAsc(MatrixPlatformWorkbenchItem::getFsection)
                        .orderByAsc(MatrixPlatformWorkbenchItem::getFsortNo)
        ).stream().collect(Collectors.groupingBy(
                MatrixPlatformWorkbenchItem::getFsection,
                LinkedHashMap::new,
                Collectors.toList()
        ));

        response.setHeroMetrics(toWorkbenchItems(sections.get("HERO_METRIC")));
        response.setTodos(toWorkbenchItems(sections.get("TODO")));
        response.setRecentItems(toWorkbenchItems(sections.get("RECENT")));
        response.setNotices(toWorkbenchItems(sections.get("NOTICE")));
        response.setQuickActions(toWorkbenchItems(sections.get("QUICK_ACTION")));
        return response;
    }

    @Override
    public List<PlatformUiItemResponse> getApps() {
        return appMapper.selectList(
                new LambdaQueryWrapper<MatrixPlatformApp>()
                        .eq(MatrixPlatformApp::getFstatus, STATUS_ENABLED)
                        .orderByAsc(MatrixPlatformApp::getFsortNo)
        ).stream().map(this::toAppItem).collect(Collectors.toList());
    }

    @Override
    public List<PlatformMenuResponse> getMenuTree(String appCode) {
        LambdaQueryWrapper<MatrixPlatformMenu> wrapper = new LambdaQueryWrapper<MatrixPlatformMenu>()
                .eq(MatrixPlatformMenu::getFstatus, STATUS_ENABLED)
                .orderByAsc(MatrixPlatformMenu::getFsortNo);
        if (StringUtils.hasText(appCode)) {
            wrapper.eq(MatrixPlatformMenu::getFappCode, appCode);
        }

        List<PlatformMenuResponse> nodes = menuMapper.selectList(wrapper).stream()
                .map(this::toMenuResponse)
                .collect(Collectors.toList());
        Map<Long, PlatformMenuResponse> byId = nodes.stream()
                .collect(Collectors.toMap(PlatformMenuResponse::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<PlatformMenuResponse> roots = new ArrayList<>();
        for (PlatformMenuResponse node : nodes) {
            if (node.getParentId() != null && byId.containsKey(node.getParentId())) {
                byId.get(node.getParentId()).getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }

    @Override
    public PlatformModuleHubResponse getModuleHub(String appCode, String moduleCode) {
        PlatformModuleHubResponse response = new PlatformModuleHubResponse();
        response.setAppCode(appCode);
        response.setModuleCode(moduleCode);

        List<MatrixPlatformModuleItem> moduleItems = moduleItemMapper.selectList(
                new LambdaQueryWrapper<MatrixPlatformModuleItem>()
                        .eq(MatrixPlatformModuleItem::getFappCode, appCode)
                        .eq(MatrixPlatformModuleItem::getFmoduleCode, moduleCode)
                        .eq(MatrixPlatformModuleItem::getFstatus, STATUS_ENABLED)
                        .orderByAsc(MatrixPlatformModuleItem::getFsection)
                        .orderByAsc(MatrixPlatformModuleItem::getFsortNo)
        );
        Map<String, List<MatrixPlatformModuleItem>> sections = moduleItems.stream().collect(Collectors.groupingBy(
                MatrixPlatformModuleItem::getFsection,
                LinkedHashMap::new,
                Collectors.toList()
        ));

        response.setStats(toModuleItems(sections.get("STAT")));
        response.setActions(toModuleItems(sections.get("ACTION")));
        response.setTopActions(toModuleItems(sections.get("TOP_ACTION")));
        response.setFocusItems(toModuleItems(sections.get("FOCUS")));
        response.setShortcuts(toModuleItems(sections.get("SHORTCUT")));
        response.setGroups(getModuleGroups(appCode, moduleCode));
        return response;
    }

    private List<PlatformModuleGroupResponse> getModuleGroups(String appCode, String moduleCode) {
        List<MatrixPlatformMenu> menus = menuMapper.selectList(
                new LambdaQueryWrapper<MatrixPlatformMenu>()
                        .eq(MatrixPlatformMenu::getFappCode, appCode)
                        .eq(MatrixPlatformMenu::getFmoduleCode, moduleCode)
                        .eq(MatrixPlatformMenu::getFstatus, STATUS_ENABLED)
                        .orderByAsc(MatrixPlatformMenu::getFsortNo)
        );

        Map<Long, List<MatrixPlatformMenu>> childrenByParent = menus.stream()
                .filter(item -> item.getFparentId() != null)
                .collect(Collectors.groupingBy(MatrixPlatformMenu::getFparentId, LinkedHashMap::new, Collectors.toList()));

        return menus.stream()
                .filter(item -> "GROUP".equalsIgnoreCase(nullToEmpty(item.getFmenuType())))
                .sorted(Comparator.comparing(item -> defaultSort(item.getFsortNo())))
                .map(group -> {
                    PlatformModuleGroupResponse response = new PlatformModuleGroupResponse();
                    response.setName(group.getFname());
                    response.setSummary(group.getFsummary());
                    response.setEyebrow(group.getFeyebrow());
                    response.setIconKey(group.getFiconKey());
                    response.setModules(childrenByParent.getOrDefault(group.getFid(), List.of()).stream()
                            .sorted(Comparator.comparing(item -> defaultSort(item.getFsortNo())))
                            .map(this::toMenuItem)
                            .collect(Collectors.toList()));
                    return response;
                })
                .collect(Collectors.toList());
    }

    private PlatformUiItemResponse toAppItem(MatrixPlatformApp app) {
        PlatformUiItemResponse item = new PlatformUiItemResponse();
        item.setKey(app.getFappCode());
        item.setName(app.getFname());
        item.setTitle(app.getFname());
        item.setDesc(app.getFdescription());
        item.setDescription(app.getFdescription());
        item.setMeta(app.getFmeta());
        item.setStatus(app.getFstatusText());
        item.setPath(app.getFroutePath());
        item.setRoutePath(app.getFroutePath());
        item.setIconKey(app.getFiconKey());
        item.setAccent(app.getFaccent());
        item.setAvailable(toBoolean(app.getFavailable(), true));
        item.setReady(item.getAvailable());
        item.setFeatured(toBoolean(app.getFfeatured(), false));
        item.setNewPage(toBoolean(app.getFnewPage(), false));
        return item;
    }

    private List<PlatformUiItemResponse> toWorkbenchItems(List<MatrixPlatformWorkbenchItem> source) {
        if (source == null) {
            return List.of();
        }
        return source.stream().map(this::toWorkbenchItem).collect(Collectors.toList());
    }

    private PlatformUiItemResponse toWorkbenchItem(MatrixPlatformWorkbenchItem source) {
        PlatformUiItemResponse item = new PlatformUiItemResponse();
        item.setKey(source.getFsection() + ":" + source.getFid());
        item.setName(source.getFname());
        item.setLabel(source.getFname());
        item.setTitle(source.getFname());
        item.setDesc(source.getFdescription());
        item.setDescription(source.getFdescription());
        item.setDetail(source.getFdescription());
        item.setValue(source.getFvalue());
        item.setHint(source.getFhint());
        item.setTag(source.getFtag());
        item.setType(source.getFitemType());
        item.setPriority(source.getFpriority());
        item.setStatus(source.getFstatusText());
        item.setMeta(source.getFhint());
        item.setTime(source.getFvalue());
        item.setPath(source.getFroutePath());
        item.setRoutePath(source.getFroutePath());
        item.setIconKey(source.getFiconKey());
        item.setAccent(source.getFaccent());
        item.setAvailable(toBoolean(source.getFavailable(), true));
        item.setReady(item.getAvailable());
        item.setNewPage(toBoolean(source.getFnewPage(), false));
        item.setFeatured(toBoolean(source.getFfeatured(), false));
        return item;
    }

    private List<PlatformUiItemResponse> toModuleItems(List<MatrixPlatformModuleItem> source) {
        if (source == null) {
            return List.of();
        }
        return source.stream().map(this::toModuleItem).collect(Collectors.toList());
    }

    private PlatformUiItemResponse toModuleItem(MatrixPlatformModuleItem source) {
        PlatformUiItemResponse item = new PlatformUiItemResponse();
        item.setKey(source.getFsection() + ":" + source.getFid());
        item.setName(source.getFname());
        item.setLabel(source.getFname());
        item.setTitle(source.getFname());
        item.setDesc(source.getFdescription());
        item.setDescription(source.getFdescription());
        item.setDetail(source.getFdescription());
        item.setValue(source.getFvalue());
        item.setHint(source.getFhint());
        item.setStatus(StringUtils.hasText(source.getFstatusText()) ? source.getFstatusText() : source.getFstatus());
        item.setPath(source.getFroutePath());
        item.setRoutePath(source.getFroutePath());
        item.setIconKey(source.getFiconKey());
        item.setPrimary(toBoolean(source.getFprimaryFlag(), false));
        item.setAvailable(true);
        item.setReady(true);
        return item;
    }

    private PlatformUiItemResponse toMenuItem(MatrixPlatformMenu menu) {
        PlatformUiItemResponse item = new PlatformUiItemResponse();
        item.setKey(menu.getFmenuCode());
        item.setName(menu.getFname());
        item.setLabel(menu.getFname());
        item.setTitle(menu.getFname());
        item.setDesc(menu.getFdescription());
        item.setDescription(menu.getFdescription());
        item.setDetail(menu.getFdescription());
        item.setStatus(menu.getFstatusText());
        item.setPath(menu.getFroutePath());
        item.setRoutePath(menu.getFroutePath());
        item.setIconKey(menu.getFiconKey());
        item.setAvailable(toBoolean(menu.getFavailable(), true));
        item.setReady(item.getAvailable());
        return item;
    }

    private PlatformMenuResponse toMenuResponse(MatrixPlatformMenu menu) {
        PlatformMenuResponse response = new PlatformMenuResponse();
        response.setId(menu.getFid());
        response.setParentId(menu.getFparentId());
        response.setAppCode(menu.getFappCode());
        response.setModuleCode(menu.getFmoduleCode());
        response.setMenuCode(menu.getFmenuCode());
        response.setName(menu.getFname());
        response.setTitle(menu.getFname());
        response.setDesc(menu.getFdescription());
        response.setDescription(menu.getFdescription());
        response.setSummary(menu.getFsummary());
        response.setEyebrow(menu.getFeyebrow());
        response.setMenuType(menu.getFmenuType());
        response.setPath(menu.getFroutePath());
        response.setRoutePath(menu.getFroutePath());
        response.setIconKey(menu.getFiconKey());
        response.setStatus(menu.getFstatusText());
        response.setAvailable(toBoolean(menu.getFavailable(), true));
        response.setReady(response.getAvailable());
        return response;
    }

    private Boolean toBoolean(Integer value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value != 0;
    }

    private Integer defaultSort(Integer value) {
        return Objects.requireNonNullElse(value, 0);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
