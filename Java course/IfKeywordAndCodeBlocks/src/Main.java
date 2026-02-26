public class Main {
    public static void main(String[] args) {
        boolean gameOver = true;
        int score = 900;
        int levelCompleted = 5;
        int bonus = 100;

        int finalScore = score;
        
        calculateScore(gameOver, score, levelCompleted, bonus);

        score = 10000;
        levelCompleted = 8;
        bonus = 200;
        finalScore = score;
        
        if (gameOver) {
            finalScore += (levelCompleted * bonus);
            System.out.println("Your new final score is " + finalScore);
        }
    }

    public static void calculateScore(boolean gameOver, int score, int levelCompleted, int bonus) {
        
        int finalScore = score;
        
        if (gameOver) {
            finalScore += (levelCompleted * bonus);
            System.out.println("Your final score was " + finalScore);
        }
    }
}
