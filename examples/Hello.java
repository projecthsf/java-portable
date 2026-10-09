// A standalone .java file — Java Portable runs it with single-file source launch,
// so there is no compile step and no project. Click the green ▶ next to main.

import java.util.List;

/** Javadoc highlights differently from an ordinary block comment. */
public class Hello {

    /* An ordinary block comment. */
    record Greeting(String name, int times) {
        String render() {
            return "Hello, %s!".formatted(name);
        }
    }

    @FunctionalInterface
    interface Shout {
        String apply(String s);
    }

    static final String BANNER = """
            A text block spans lines and may contain "quotes"
            without escaping any of them.
            """;

    public static void main(String[] args) {
        var greeting = new Greeting("Java", 2);
        Shout shout = s -> s.toUpperCase();

        for (int i = 1; i <= greeting.times(); i++) {
            System.out.println(i + ". " + greeting.render());
        }

        List.of("portable", "no install").forEach(s -> System.out.println(shout.apply(s)));

        int total = 10, count = 4;
        System.out.printf("avg=%d hex=%X big=%d chr=%c%n", total / count, 255, 1_000_000, 'J');
        System.out.print(BANNER);
    }
}
