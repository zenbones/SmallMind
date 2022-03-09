package org.smallmind.memcached.cubby;

public class PrimeGenerator {

  // Function that returns true if n
  // is prime else returns false
  private static boolean isPrime (int n) {
    // Corner cases
    if (n <= 1) {

      return false;
    } else if (n <= 3) {

      return true;

      // This is checked so that we can skip
      // middle five numbers in below loop
    } else {
      if ((n % 2 == 0) || (n % 3 == 0)) {

        return false;
      } else {
        for (int i = 5; i * i <= n; i = i + 6) {
          if ((n % i == 0) || (n % (i + 2) == 0)) {

            return false;
          }
        }

        return true;
      }
    }
  }

  // Function to return the smallest
  // prime number greater than N
  public static int nextPrime (int n) {

    // Base case
    if (n <= 1) {

      return 2;
    } else {

      int prime = n;

      // Loop continuously until isPrime returns
      // true for a number greater than n
      while (true) {
        if (isPrime(++prime)) {

          return prime;
        }
      }
    }
  }

  // Driver code
  public static void main (String[] args) {

    int N = 701;

    System.out.println(nextPrime(N));
  }
}
