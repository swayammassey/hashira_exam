# Shamir Secret Interpolation (No Python)

A tiny Node.js CLI that reads a JSON test case with points in mixed bases and computes the secret (constant term f(0)) using Lagrange interpolation.

## Requirements
- Node.js 16+ installed

## Install / Run
```
# From the project root
node src/index.js samples/sample1.json

# Or using the package script
npm start
```

## JSON Format
```json
{
  "keys": { "n": 4, "k": 3 },
  "1": { "base": "10", "value": "4" },
  "2": { "base": "2",  "value": "111" },
  "3": { "base": "10", "value": "12" },
  "6": { "base": "4",  "value": "213" }
}
```
- `keys.n`: total number of points
- `keys.k`: threshold (degree + 1)
- Each other key is the x-coordinate; its `value` is the y-coordinate expressed in the given `base`.

## How it works
- Parses y-values according to their base into BigInt integers.
- Selects the first `k` points by ascending x.
- Applies Lagrange interpolation and evaluates f(0).
- Outputs the secret as an exact fraction (reduced). If it is an integer, it prints just the integer.

## Sample Output (for `samples/sample1.json`)
```
k = 3
selected points = (1, 4), (2, 7), (3, 12)
secret f(0) = 3
```

## Test Cases

### Sample 1 (samples/sample1.json)
```
npm run sample1
```
Expected output:
```
k = 3
selected points = (1, 4), (2, 7), (3, 12)
secret f(0) = 3
```

### Sample 2 (samples/sample2.json)
```
npm run sample2
```
Expected output:
```
k = 7
selected points = (4, 1016509518118225951), (5, 3711974121218449851), (6, 10788619898233492461), (7, 26709394976508342463), (8, 58725075613853308713), (9, 117852986202006511971), (10, 220003896831595324801)
secret f(0) = 79836264059301
```
This test case uses larger numbers in various bases (3, 6, 7, 8, 12, 15, 16) with k=7 points needed. Uses points 4-10 for correct interpolation.

## Use with your own test file
Place your JSON file anywhere and run:
```
node src/index.js path/to/your.json
```

## Java Version
Alternative Java implementation available:
```bash
# Compile and run
javac ShamirJava.java
java ShamirJava samples/sample1.json  # outputs: secret f(0) = 3
java ShamirJava samples/sample2.json  # outputs: secret f(0) = 79836264059301
```

## Notes
- This implementation computes over the rationals (no finite field modulus).
- Node.js version uses BigInt-backed exact fractions to avoid precision loss.
- Java version uses BigInteger with custom Fraction class for exact arithmetic.
