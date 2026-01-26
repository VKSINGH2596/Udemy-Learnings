public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello, Vaibhav");
        
        boolean isAlien = false;
        if (isAlien == false) {
            System.out.println("It is not an alien!");
            System.out.println("And I am scared of aliens");
        }

        int topScore = 100;
        if (topScore < 100) {
            System.out.println("You got less than high score!");
        }

        if (topScore <= 100) {
            System.out.println("You got equal or under high score");
        }

        int secondTopScore = 60;
        if ((topScore > secondTopScore) && (topScore < 100)) {      // logical AND operator
            System.out.println("Greater than second top score and less than 100");
        }

        if ((topScore > 90) || (secondTopScore <= 90)) {        // logical OR operator
            System.out.println("Either or both of the conditions are true");
        }

        int newValue = 50;
        if (newValue == 50) {       // using single equal sign for assignment, double equal sign for comparison
            System.out.println("This is an Error");
        }

        boolean isCar = false;
        if (isCar) {            // negation operator
            System.out.println("This is not supposed to happen");
        }
    }
}
