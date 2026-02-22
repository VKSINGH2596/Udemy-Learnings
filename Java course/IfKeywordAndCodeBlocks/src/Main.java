public class Main {
    public static void main(String[] args) {
        boolean gameOver = true;
        int score = 500;
        int levelCompleted = 6;
        int bonus = 100;

        if (score > 600) {
            System.out.println("Your score was greater than 600");
        } else if (score == 600) {
            System.out.println("Your score was 600");
        } else {
            System.out.println("Your score was less than 600");
        }
    }
}
