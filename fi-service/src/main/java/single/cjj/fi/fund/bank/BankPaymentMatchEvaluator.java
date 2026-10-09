package single.cjj.fi.fund.bank;

import org.springframework.util.StringUtils;
import single.cjj.fi.fund.bank.BankTransactionContracts.Difference;
import single.cjj.fi.fund.payment.PaymentOrderRepository.PaymentOrderRow;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BankPaymentMatchEvaluator {

    public List<Difference> evaluate(
            PaymentOrderRow order,
            BankTransactionRepository.BankTransactionRow bank
    ) {
        List<Difference> differences = new ArrayList<>();

        if (!"OUTBOUND".equals(bank.direction())) {
            differences.add(new Difference(
                    "DIRECTION_DIFFERENCE",
                    "direction",
                    "OUTBOUND",
                    bank.direction(),
                    "银行流水方向必须为付款流出"
            ));
        }
        compare(
                differences,
                "BANK_ACCOUNT_DIFFERENCE",
                "bankAccountId",
                order.payerBankAccountId(),
                bank.bankAccountId(),
                true,
                "付款方银行账户不一致"
        );
        compare(
                differences,
                "CURRENCY_DIFFERENCE",
                "currencyCode",
                order.currencyCode(),
                bank.currencyCode(),
                true,
                "付款单与银行流水币种不一致"
        );
        if (money(order.amount()).compareTo(money(bank.amount())) != 0) {
            differences.add(new Difference(
                    "AMOUNT_DIFFERENCE",
                    "amount",
                    money(order.amount()).toPlainString(),
                    money(bank.amount()).toPlainString(),
                    "付款单与银行流水金额不一致"
            ));
        }
        if (StringUtils.hasText(order.payeeBankAccountNo())
                && StringUtils.hasText(bank.counterpartyAccount())
                && !Objects.equals(
                        order.payeeBankAccountNo().trim(),
                        bank.counterpartyAccount().trim())) {
            differences.add(new Difference(
                    "COUNTERPARTY_ACCOUNT_DIFFERENCE",
                    "counterpartyAccount",
                    order.payeeBankAccountNo(),
                    bank.counterpartyAccount(),
                    "付款单收款账号与银行流水对手账号不一致"
            ));
        }
        return List.copyOf(differences);
    }

    private void compare(
            List<Difference> differences,
            String code,
            String field,
            String expected,
            String actual,
            boolean required,
            String message
    ) {
        boolean expectedPresent = StringUtils.hasText(expected);
        boolean actualPresent = StringUtils.hasText(actual);
        if (required && (!expectedPresent || !actualPresent)) {
            differences.add(new Difference(
                    code,
                    field,
                    expected,
                    actual,
                    message + "（必填字段缺失）"
            ));
            return;
        }
        if (expectedPresent && actualPresent && !Objects.equals(expected.trim(), actual.trim())) {
            differences.add(new Difference(code, field, expected, actual, message));
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2);
    }
}
