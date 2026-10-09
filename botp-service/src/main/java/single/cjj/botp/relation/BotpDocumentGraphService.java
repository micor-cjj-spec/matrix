package single.cjj.botp.relation;

import org.springframework.stereotype.Service;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentGraph;
import single.cjj.botp.domain.BotpContracts.DocumentGraphEdge;
import single.cjj.botp.domain.BotpContracts.DocumentGraphNode;
import single.cjj.botp.domain.BotpContracts.DocumentKey;
import single.cjj.botp.domain.BotpContracts.DocumentRelation;
import single.cjj.botp.domain.BotpContracts.DocumentRelationEntry;
import single.cjj.botp.domain.BotpContracts.RelationStatus;

import java.math.BigDecimal;
import java.util.*;

@Service
public class BotpDocumentGraphService {

    private static final int MAX_DEPTH = 10;
    private static final int MAX_NODES = 500;

    private final BotpRelationRepository repository;

    public BotpDocumentGraphService(BotpRelationRepository repository) {
        this.repository = repository;
    }

    public DocumentGraph graph(
            String tenantId,
            String systemCode,
            String documentType,
            String documentId,
            int requestedDepth
    ) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new BizException("tenantId 不能为空");
        }
        int maxDepth = Math.max(0, Math.min(requestedDepth, MAX_DEPTH));
        DocumentKey root = new DocumentKey(
                tenantId.trim(), require(systemCode, "systemCode"),
                require(documentType, "documentType"),
                require(documentId, "documentId"));

        Map<DocumentKey, DocumentGraphNode> nodes = new LinkedHashMap<>();
        Map<Long, DocumentGraphEdge> edges = new LinkedHashMap<>();
        Set<DocumentKey> visited = new LinkedHashSet<>();
        ArrayDeque<QueueNode> queue = new ArrayDeque<>();

        nodes.put(root, new DocumentGraphNode(root, null));
        queue.add(new QueueNode(root, 0));
        boolean truncated = false;
        int reachedDepth = 0;

        while (!queue.isEmpty()) {
            QueueNode current = queue.removeFirst();
            if (!visited.add(current.key())) {
                continue;
            }
            reachedDepth = Math.max(reachedDepth, current.depth());
            if (current.depth() >= maxDepth) {
                continue;
            }

            List<DocumentRelation> relations = new ArrayList<>();
            relations.addAll(repository.findBySource(
                    current.key().tenantId(),
                    current.key().systemCode(),
                    current.key().documentType(),
                    current.key().documentId()));
            relations.addAll(repository.findByTarget(
                    current.key().tenantId(),
                    current.key().systemCode(),
                    current.key().documentType(),
                    current.key().documentId()));

            for (DocumentRelation relation : relations) {
                DocumentKey source = relation.sourceDocument().key(relation.tenantId());
                DocumentKey target = new DocumentKey(
                        relation.tenantId(),
                        relation.targetDocument().systemCode(),
                        relation.targetDocument().documentType(),
                        relation.targetDocument().documentId());

                BigDecimal quantity = BigDecimal.ZERO;
                BigDecimal amount = BigDecimal.ZERO;
                for (DocumentRelationEntry entry : repository.findEntries(relation.relationId())) {
                    if (entry.status() != RelationStatus.ACTIVE
                            && entry.status() != RelationStatus.PENDING) {
                        continue;
                    }
                    if (entry.quantity() != null) {
                        quantity = quantity.add(entry.quantity());
                    }
                    if (entry.amount() != null) {
                        amount = amount.add(entry.amount());
                    }
                }

                edges.putIfAbsent(relation.relationId(), new DocumentGraphEdge(
                        relation.relationId(), source, target, relation.status(),
                        quantity, amount));

                nodes.putIfAbsent(source, new DocumentGraphNode(source, null));
                DocumentGraphNode existingTarget = nodes.get(target);
                if (existingTarget == null
                        || existingTarget.documentNo() == null
                        || existingTarget.documentNo().isBlank()) {
                    nodes.put(target, new DocumentGraphNode(
                            target, relation.targetDocument().documentNo()));
                }

                if (nodes.size() >= MAX_NODES) {
                    truncated = true;
                    break;
                }
                if (!visited.contains(source)) {
                    queue.addLast(new QueueNode(source, current.depth() + 1));
                }
                if (!visited.contains(target)) {
                    queue.addLast(new QueueNode(target, current.depth() + 1));
                }
            }
            if (truncated) {
                break;
            }
        }

        return new DocumentGraph(
                root,
                List.copyOf(nodes.values()),
                List.copyOf(edges.values()),
                reachedDepth,
                truncated
        );
    }

    public List<DocumentRelation> upstream(DocumentKey key) {
        return repository.findByTarget(
                key.tenantId(), key.systemCode(),
                key.documentType(), key.documentId());
    }

    public List<DocumentRelation> downstream(DocumentKey key) {
        return repository.findBySource(
                key.tenantId(), key.systemCode(),
                key.documentType(), key.documentId());
    }

    private String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BizException(field + " 不能为空");
        }
        return value.trim();
    }

    private record QueueNode(DocumentKey key, int depth) {
    }
}
