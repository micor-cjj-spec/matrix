import java.math.BigDecimal;

public class Main {

    // 循环次数，按你机器性能可调
    private static final int COUNT = 10_000_000;

    public static void main(String[] args) {
        // 预热，避免第一次执行受 JIT 影响太大
        for (int i = 0; i < 3; i++) {
            testDoubleAdd();
            testDoubleSub();
            testBigDecimalAdd();
            testBigDecimalSub();
        }

        System.out.println("========== 正式测试开始 ==========");

        long doubleAddTime = testDoubleAdd();
        long doubleSubTime = testDoubleSub();
        long bigDecimalAddTime = testBigDecimalAdd();
        long bigDecimalSubTime = testBigDecimalSub();

        System.out.println("\n========== 测试结果 ==========");
        System.out.println("double 加法耗时:      " + doubleAddTime + " ms");
        System.out.println("double 减法耗时:      " + doubleSubTime + " ms");
        System.out.println("BigDecimal 加法耗时: " + bigDecimalAddTime + " ms");
        System.out.println("BigDecimal 减法耗时: " + bigDecimalSubTime + " ms");
    }

    private static long testDoubleAdd() {
        double a = 12345.67;
        double b = 76543.21;
        double result = 0.0;

        long start = System.nanoTime();
        for (int i = 0; i < COUNT; i++) {
            result += a + b;
        }
        long end = System.nanoTime();

        // 防止JIT过度优化
        if (result == 0) {
            System.out.println("ignore: " + result);
        }

        return (end - start) / 1_000_000;
    }

    private static long testDoubleSub() {
        double a = 12345.67;
        double b = 76543.21;
        double result = 0.0;

        long start = System.nanoTime();
        for (int i = 0; i < COUNT; i++) {
            result += a - b;
        }
        long end = System.nanoTime();

        if (result == 0) {
            System.out.println("ignore: " + result);
        }

        return (end - start) / 1_000_000;
    }

    private static long testBigDecimalAdd() {
        BigDecimal a = new BigDecimal("12345.67");
        BigDecimal b = new BigDecimal("76543.21");
        BigDecimal result = BigDecimal.ZERO;

        long start = System.nanoTime();
        for (int i = 0; i < COUNT; i++) {
            result = result.add(a.add(b));
        }
        long end = System.nanoTime();

        if (result.compareTo(BigDecimal.ZERO) == 0) {
            System.out.println("ignore: " + result);
        }

        return (end - start) / 1_000_000;
    }

    private static long testBigDecimalSub() {
        BigDecimal a = new BigDecimal("12345.67");
        BigDecimal b = new BigDecimal("76543.21");
        BigDecimal result = BigDecimal.ZERO;

        long start = System.nanoTime();
        for (int i = 0; i < COUNT; i++) {
            result = result.add(a.subtract(b));
        }
        long end = System.nanoTime();

        if (result.compareTo(BigDecimal.ZERO) == 0) {
            System.out.println("ignore: " + result);
        }

        return (end - start) / 1_000_000;
    }
}