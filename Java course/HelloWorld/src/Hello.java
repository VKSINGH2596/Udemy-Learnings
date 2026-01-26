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

        String makeOfCar = "Volkswagen";
        boolean isDomestic = makeOfCar == "Volkswagen" ? false : true;   // ternary operator
        if (isDomestic) {
            System.out.println("This car is domestic to our country");
        }

        // Challenge 1
        double firstValue = 20.00d;
        double secondValue = 80.00d;
        double totalValue = (firstValue + secondValue) * 100.00d;
        System.out.println("\nTotal Value: " + totalValue);
        double remainder = totalValue % 40.00d;
        System.out.println("Remainder: " + remainder);
        boolean isNoRemainder = (remainder == 0) ? true : false;
        System.out.println("Is there no remainder? " + isNoRemainder);
        if (!isNoRemainder) {
            System.out.println("Got some remainder");
        }
    }
}
