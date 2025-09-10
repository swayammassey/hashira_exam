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
selected points = (1, 1284755224093748462846905269271565318441), (2, 4377027755248439082937867), (3, 3492461), (4, 4135903920539906487), (5, 4742473784338349815), (6, 279734996209612467957165), (7, 2670177736758346)
secret f(0) = -6290016743746469796
```
This test case uses larger numbers in various bases (3, 6, 7, 8, 12, 15, 16) with k=7 points needed.

## Use with your own test file
Place your JSON file anywhere and run:
```
node src/index.js path/to/your.json
```

## Notes
- This implementation computes over the rationals (no finite field modulus).
- Arithmetic uses BigInt-backed exact fractions to avoid precision loss.
