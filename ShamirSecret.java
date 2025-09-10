import java.io.*;
import java.math.BigInteger;
import java.util.*;
import com.google.gson.*;

public class ShamirSecret {
    
    // Fraction class for exact arithmetic
    static class Fraction {
        private BigInteger numerator;
        private BigInteger denominator;
        
        public Fraction(BigInteger n, BigInteger d) {
            if (d.equals(BigInteger.ZERO)) {
                throw new ArithmeticException("Division by zero");
            }
            
            // Normalize sign
            if (d.compareTo(BigInteger.ZERO) < 0) {
                n = n.negate();
                d = d.negate();
            }
            
            // Reduce fraction
            BigInteger gcd = n.abs().gcd(d);
            this.numerator = n.divide(gcd);
            this.denominator = d.divide(gcd);
        }
        
        public Fraction(BigInteger n) {
            this(n, BigInteger.ONE);
        }
        
        public Fraction(long n) {
            this(BigInteger.valueOf(n), BigInteger.ONE);
        }
        
        public Fraction add(Fraction other) {
            BigInteger newNum = this.numerator.multiply(other.denominator)
                              .add(other.numerator.multiply(this.denominator));
            BigInteger newDen = this.denominator.multiply(other.denominator);
            return new Fraction(newNum, newDen);
        }
        
        public Fraction subtract(Fraction other) {
            BigInteger newNum = this.numerator.multiply(other.denominator)
                              .subtract(other.numerator.multiply(this.denominator));
            BigInteger newDen = this.denominator.multiply(other.denominator);
            return new Fraction(newNum, newDen);
        }
        
        public Fraction multiply(Fraction other) {
            return new Fraction(
                this.numerator.multiply(other.numerator),
                this.denominator.multiply(other.denominator)
            );
        }
        
        public Fraction divide(Fraction other) {
            if (other.numerator.equals(BigInteger.ZERO)) {
                throw new ArithmeticException("Division by zero");
            }
            return new Fraction(
                this.numerator.multiply(other.denominator),
                this.denominator.multiply(other.numerator)
            );
        }
        
        public boolean isInteger() {
            return denominator.equals(BigInteger.ONE);
        }
        
        @Override
        public String toString() {
            if (isInteger()) {
                return numerator.toString();
            }
            return numerator + "/" + denominator;
        }
    }
    
    // Point class to store (x, y) coordinates
    static class Point {
        BigInteger x;
        BigInteger y;
        
        Point(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
        
        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }
    
    // Parse integer from given base
    public static BigInteger parseFromBase(String value, int base) {
        if (base < 2 || base > 36) {
            throw new IllegalArgumentException("Unsupported base: " + base);
        }
        
        BigInteger result = BigInteger.ZERO;
        BigInteger baseBI = BigInteger.valueOf(base);
        String digits = "0123456789abcdefghijklmnopqrstuvwxyz";
        
        for (char c : value.toLowerCase().toCharArray()) {
            int digitValue = digits.indexOf(c);
            if (digitValue < 0 || digitValue >= base) {
                throw new IllegalArgumentException("Invalid digit '" + c + "' for base " + base);
            }
            result = result.multiply(baseBI).add(BigInteger.valueOf(digitValue));
        }
        
        return result;
    }
    
    // Extract points from JSON
    public static List<Point> extractPoints(JsonObject json) {
        JsonObject keys = json.getAsJsonObject("keys");
        int n = keys.get("n").getAsInt();
        int k = keys.get("k").getAsInt();
        
        List<Point> points = new ArrayList<>();
        
        for (String key : json.keySet()) {
            if (key.equals("keys")) continue;
            
            JsonObject entry = json.getAsJsonObject(key);
            BigInteger x = new BigInteger(key);
            int base = entry.get("base").getAsInt();
            String value = entry.get("value").getAsString();
            BigInteger y = parseFromBase(value, base);
            
            points.add(new Point(x, y));
        }
        
        // Sort by x coordinate and take first k points
        points.sort(Comparator.comparing(p -> p.x));
        return points.subList(0, Math.min(k, points.size()));
    }
    
    // Lagrange interpolation at x = 0
    public static Fraction lagrangeAtZero(List<Point> points) {
        Fraction result = new Fraction(0);
        
        for (int i = 0; i < points.size(); i++) {
            Point pi = points.get(i);
            Fraction term = new Fraction(pi.y);
            
            for (int j = 0; j < points.size(); j++) {
                if (i == j) continue;
                
                Point pj = points.get(j);
                // term *= (0 - xj) / (xi - xj)
                Fraction numerator = new Fraction(pj.x.negate());
                Fraction denominator = new Fraction(pi.x.subtract(pj.x));
                term = term.multiply(numerator.divide(denominator));
            }
            
            result = result.add(term);
        }
        
        return result;
    }
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java ShamirSecret <json-file>");
            System.exit(1);
        }
        
        try {
            // Read JSON file
            FileReader reader = new FileReader(args[0]);
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            reader.close();
            
            // Extract points
            List<Point> points = extractPoints(json);
            
            // Calculate secret
            Fraction secret = lagrangeAtZero(points);
            
            // Output results
            System.out.println("k = " + points.size());
            System.out.print("selected points = ");
            for (int i = 0; i < points.size(); i++) {
                if (i > 0) System.out.print(", ");
                System.out.print(points.get(i));
            }
            System.out.println();
            System.out.println("secret f(0) = " + secret);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
