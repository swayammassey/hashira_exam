import java.io.*;
import java.math.BigInteger;
import java.util.*;

public class ShamirSecretSimple {
    
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
    
    // Point class
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
    
    // Simple JSON parser for our specific format
    public static Map<String, Object> parseSimpleJson(String jsonContent) {
        Map<String, Object> result = new HashMap<>();
        
        // Clean up the JSON content
        jsonContent = jsonContent.trim();
        if (jsonContent.startsWith("{")) jsonContent = jsonContent.substring(1);
        if (jsonContent.endsWith("}")) jsonContent = jsonContent.substring(0, jsonContent.length() - 1);
        
        // Parse keys section
        int keysStart = jsonContent.indexOf("\"keys\"");
        int keysEnd = jsonContent.indexOf("}", keysStart) + 1;
        String keysSection = jsonContent.substring(keysStart, keysEnd);
        
        // Extract n and k values
        Map<String, Integer> keys = new HashMap<>();
        if (keysSection.contains("\"n\"")) {
            int nStart = keysSection.indexOf("\"n\"") + 4;
            int nEnd = keysSection.indexOf(",", nStart);
            if (nEnd == -1) nEnd = keysSection.indexOf("}", nStart);
            String nValue = keysSection.substring(nStart, nEnd).replaceAll("[^0-9]", "");
            keys.put("n", Integer.parseInt(nValue));
        }
        if (keysSection.contains("\"k\"")) {
            int kStart = keysSection.indexOf("\"k\"") + 4;
            int kEnd = keysSection.indexOf("}", kStart);
            String kValue = keysSection.substring(kStart, kEnd).replaceAll("[^0-9]", "");
            keys.put("k", Integer.parseInt(kValue));
        }
        result.put("keys", keys);
        
        // Parse point sections
        String remaining = jsonContent.substring(keysEnd + 1);
        String[] pointSections = remaining.split("\\},\\s*\"");
        
        for (String section : pointSections) {
            section = section.trim();
            if (section.isEmpty()) continue;
            
            // Extract point key (x coordinate)
            int keyStart = section.indexOf("\"");
            if (keyStart == -1) keyStart = 0;
            else keyStart++;
            int keyEnd = section.indexOf("\"", keyStart);
            if (keyEnd == -1) continue;
            
            String pointKey = section.substring(keyStart, keyEnd);
            
            // Extract base and value
            Map<String, String> pointData = new HashMap<>();
            
            int baseStart = section.indexOf("\"base\"");
            if (baseStart != -1) {
                baseStart = section.indexOf("\"", baseStart + 6) + 1;
                int baseEnd = section.indexOf("\"", baseStart);
                String base = section.substring(baseStart, baseEnd);
                pointData.put("base", base);
            }
            
            int valueStart = section.indexOf("\"value\"");
            if (valueStart != -1) {
                valueStart = section.indexOf("\"", valueStart + 7) + 1;
                int valueEnd = section.indexOf("\"", valueStart);
                String value = section.substring(valueStart, valueEnd);
                pointData.put("value", value);
            }
            
            if (!pointData.isEmpty()) {
                result.put(pointKey, pointData);
            }
        }
        
        return result;
    }
    
    // Extract points from parsed JSON
    @SuppressWarnings("unchecked")
    public static List<Point> extractPoints(Map<String, Object> json) {
        Map<String, Integer> keys = (Map<String, Integer>) json.get("keys");
        int k = keys.get("k");
        
        List<Point> points = new ArrayList<>();
        
        for (String key : json.keySet()) {
            if (key.equals("keys")) continue;
            
            Map<String, String> entry = (Map<String, String>) json.get(key);
            BigInteger x = new BigInteger(key);
            int base = Integer.parseInt(entry.get("base"));
            String value = entry.get("value");
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
            System.err.println("Usage: java ShamirSecretSimple <json-file>");
            System.exit(1);
        }
        
        try {
            // Read JSON file
            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(args[0]));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            reader.close();
            
            // Parse JSON
            Map<String, Object> json = parseSimpleJson(content.toString());
            
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
