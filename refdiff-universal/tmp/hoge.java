public class hoge {
  public static void main(String[] args) {
    System.out.println("Hello, World!");
  }

  public void greet() {
    int hour = java.time.LocalTime.now().getHour();
    if (hour < 12) {
      System.out.println("Good morning!");
    } else if (hour < 18) {
      System.out.println("Good afternoon!");
    } else {
      System.out.println("Good evening!");
    }
    System.out.println("Greetings from hoge class!");
  }
}
