public class Main {
    public static void main(String[] args) {
        System.out.println("Welcome to the Main class!");
        double kilometers = (100 * 1.609344);

        int highScore = 50;

        if (highScore > 25) {
            highScore = 1000 + highScore;       // add bonus points
        }

        int health = 100;
        if ((health < 25) && (highScore > 1000)) {
            highScore = highScore - 1000;
        }

        /* In above code below are the parts which are expressions:
        - health = 100
        - health < 25
        - highScore > 1000
        - (health < 25) && (highScore > 1000)
        - highScore - 1000
        - highScore = highScore - 1000
        */

    }
}
