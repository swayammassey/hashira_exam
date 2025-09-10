#!/usr/bin/env node
import fs from 'fs';
import path from 'path';

// Fraction implementation with BigInt for exact arithmetic
class Fraction {
  constructor(n, d = 1n) {
    if (d === 0n) throw new Error('Division by zero in Fraction');
    if (typeof n === 'number') n = BigInt(n);
    if (typeof d === 'number') d = BigInt(d);
    // normalize sign
    if (d < 0n) {
      n = -n;
      d = -d;
    }
    const g = Fraction.#gcd(Fraction.#abs(n), d);
    this.n = n / g;
    this.d = d / g;
  }
  static #abs(x) { return x < 0n ? -x : x; }
  static #gcd(a, b) {
    while (b !== 0n) { const t = b; b = a % b; a = t; }
    return a === 0n ? 1n : a;
  }
  add(other) {
    return new Fraction(this.n * other.d + other.n * this.d, this.d * other.d);
  }
  sub(other) {
    return new Fraction(this.n * other.d - other.n * this.d, this.d * other.d);
  }
  mul(other) {
    return new Fraction(this.n * other.n, this.d * other.d);
  }
  div(other) {
    if (other.n === 0n) throw new Error('Division by zero in Fraction.div');
    return new Fraction(this.n * other.d, this.d * other.n);
  }
  neg() { return new Fraction(-this.n, this.d); }
  isInteger() { return this.d === 1n; }
  toString() {
    if (this.d === 1n) return this.n.toString();
    return `${this.n.toString()}/${this.d.toString()}`;
  }
}

function parseIntInBase(str, base) {
  // base as string or number; supports up to base 36 like parseInt, but returns BigInt
  if (typeof base === 'string') base = Number(base);
  if (!Number.isInteger(base) || base < 2 || base > 36) {
    throw new Error(`Unsupported base: ${base}`);
  }
  const digits = str.trim();
  const alphabet = '0123456789abcdefghijklmnopqrstuvwxyz';
  let value = 0n;
  for (const ch of digits.toLowerCase()) {
    const idx = alphabet.indexOf(ch);
    if (idx < 0 || idx >= base) {
      throw new Error(`Invalid digit '${ch}' for base ${base} in value '${str}'`);
    }
    value = value * BigInt(base) + BigInt(idx);
  }
  return value;
}

function readJson(filePath) {
  const abs = path.resolve(process.cwd(), filePath);
  const raw = fs.readFileSync(abs, 'utf8');
  return JSON.parse(raw);
}

function extractPoints(obj) {
  if (!obj.keys || typeof obj.keys.n !== 'number' || typeof obj.keys.k !== 'number') {
    throw new Error('JSON must contain keys.n and keys.k as numbers');
  }
  const n = obj.keys.n;
  const k = obj.keys.k;
  const points = [];
  for (const key of Object.keys(obj)) {
    if (key === 'keys') continue;
    const xStr = key;
    const entry = obj[key];
    if (!entry || typeof entry.base === 'undefined' || typeof entry.value === 'undefined') {
      continue;
    }
    const x = BigInt(xStr);
    const y = parseIntInBase(entry.value, entry.base);
    points.push({ x, y });
  }
  if (points.length !== n) {
    // not fatal, but warn
    console.warn(`Warning: keys.n = ${n} but found ${points.length} points in JSON`);
  }
  // sort by x and select first k
  points.sort((a, b) => (a.x < b.x ? -1 : a.x > b.x ? 1 : 0));
  return { k, points: points.slice(0, k) };
}

function lagrangeAtZero(points) {
  // f(0) = sum_i y_i * prod_{j!=i} (-x_j)/(x_i - x_j)
  let sum = new Fraction(0n, 1n);
  for (let i = 0; i < points.length; i++) {
    const xi = points[i].x;
    const yi = points[i].y;
    let term = new Fraction(yi, 1n);
    for (let j = 0; j < points.length; j++) {
      if (j === i) continue;
      const xj = points[j].x;
      const num = new Fraction(-xj, 1n);
      const den = new Fraction(xi - xj, 1n);
      term = term.mul(num).div(den);
    }
    sum = sum.add(term);
  }
  return sum;
}

function main() {
  const file = process.argv[2];
  if (!file) {
    console.error('Usage: secret <path-to-json>');
    process.exit(1);
  }
  const data = readJson(file);
  const { k, points } = extractPoints(data);
  if (points.length < k) {
    throw new Error(`Not enough points to interpolate: have ${points.length}, need ${k}`);
  }
  const secret = lagrangeAtZero(points);
  console.log('k =', k);
  console.log('selected points =', points.map(p => `(${p.x.toString()}, ${p.y.toString()})`).join(', '));
  console.log('secret f(0) =', secret.toString());
  if (!secret.isInteger()) {
    // Also show decimal (rounded) for convenience
    const num = Number(secret.n);
    const den = Number(secret.d);
    if (Number.isFinite(num) && Number.isFinite(den) && den !== 0) {
      console.log('approx ≈', (num / den));
    }
  }
}

main();
