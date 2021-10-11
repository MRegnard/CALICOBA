/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.irit.smac.calicoba.test_util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * Implementation of a Sobol sequence.
 * <p>
 * A Sobol sequence is a low-discrepancy sequence with the property that for all
 * values of N, its subsequence (x1, ... xN) has a low discrepancy. It can be
 * used to generate pseudo-random points in a space S, which are
 * equi-distributed.
 * <p>
 * The implementation already comes with support for up to 1000 dimensions with
 * direction numbers calculated from
 * <a href="http://web.maths.unsw.edu.au/~fkuo/sobol/">Stephen Joe and Frances
 * Kuo</a>.
 * <p>
 *
 * @see <a href="http://en.wikipedia.org/wiki/Sobol_sequence">Sobol sequence
 *      (Wikipedia)</a>
 * @see <a href="http://web.maths.unsw.edu.au/~fkuo/sobol/">Sobol sequence
 *      direction numbers</a>
 *
 * @version $Id: SobolSequenceGenerator.html 908881 2014-05-15 07:10:28Z luc $
 * @since 3.3
 */
public class SobolSequenceGenerator implements Iterable<double[]> {
  /** The number of bits to use. */
  private static final int BITS = 52;

  /** The scaling factor. */
  private static final double SCALE = Math.pow(2, BITS);

  /** The maximum supported space dimension. */
  private static final int MAX_DIMENSION = 1000;

  /** The resource containing the direction numbers. */
  private static final String RESOURCE_NAME = "/fr/irit/smac/calicoba/test_util/new-joe-kuo-6.1000";

  /** Character set for file input. */
  private static final String FILE_CHARSET = "US-ASCII";

  /** Space dimension. */
  private final int dimension;

  /** Maximum number of values to generate. */
  private final int nb;

  /** The current index in the sequence. */
  private int count;

  /** The direction vector for each component. */
  private final long[][] direction;

  /** The current state. */
  private final long[] x;

  /**
   * Construct a new Sobol sequence generator for the given space dimension.
   *
   * @param dimension The space dimension.
   * @param nb        The maximum number of vectors to generate.
   * @throws OutOfRangeException If the space dimension is outside the allowed
   *                             range of [1, 1000].
   */
  public SobolSequenceGenerator(final int dimension, final int nb) {
    if (dimension < 1 || dimension > MAX_DIMENSION) {
      throw new IllegalArgumentException(String.format("dimension should be in [1, %d]", MAX_DIMENSION));
    }

    // initialize the other dimensions with direction numbers from a resource
    final InputStream is = this.getClass().getResourceAsStream(RESOURCE_NAME);
    if (is == null) {
      throw new RuntimeException("resource not found");
    }

    this.dimension = dimension;
    this.nb = nb;

    // init data structures
    this.direction = new long[dimension][BITS + 1];
    this.x = new long[dimension];

    try {
      this.initFromStream(is);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        is.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }

  /**
   * Load the direction vector for each dimension from the given stream.
   * <p>
   * The input stream <i>must</i> be an ASCII text containing one valid direction
   * vector per line.
   *
   * @param is the input stream to read the direction vector from
   * @return the last dimension that has been read from the input stream
   * @throws IOException        if the stream could not be read
   * @throws MathParseException if the content could not be parsed successfully
   */
  private int initFromStream(final InputStream is) throws IOException {

    // special case: dimension 1 -> use unit initialization
    for (int i = 1; i <= BITS; i++) {
      this.direction[0][i] = 1l << (BITS - i);
    }

    final Charset charset = Charset.forName(FILE_CHARSET);
    final BufferedReader reader = new BufferedReader(new InputStreamReader(is, charset));
    int dim = -1;

    try {
      // ignore first line
      reader.readLine();

      int index = 1;
      String line = null;
      while ((line = reader.readLine()) != null) {
        StringTokenizer st = new StringTokenizer(line, " ");
        dim = Integer.parseInt(st.nextToken());
        if (dim >= 2 && dim <= this.dimension) { // we have found the right dimension
          final int s = Integer.parseInt(st.nextToken());
          final int a = Integer.parseInt(st.nextToken());
          final int[] m = new int[s + 1];
          for (int i = 1; i <= s; i++) {
            m[i] = Integer.parseInt(st.nextToken());
          }
          this.initDirectionVector(index, a, m);
          index++;
        }

        if (dim > this.dimension) {
          return dim;
        }
      }
    } finally {
      reader.close();
    }

    return dim;
  }

  /**
   * Calculate the direction numbers from the given polynomial.
   *
   * @param d the dimension, zero-based
   * @param a the coefficients of the primitive polynomial
   * @param m the initial direction numbers
   */
  private void initDirectionVector(final int d, final int a, final int[] m) {
    final int s = m.length - 1;
    for (int i = 1; i <= s; i++) {
      this.direction[d][i] = ((long) m[i]) << (BITS - i);
    }
    for (int i = s + 1; i <= BITS; i++) {
      this.direction[d][i] = this.direction[d][i - s] ^ (this.direction[d][i - s] >> s);
      for (int j = 1; j < s; j++) {
        this.direction[d][i] ^= ((a >> (s - 1 - j)) & 1) * this.direction[d][i - j];
      }
    }
  }

  /**
   * Calculates then returns the next vector.
   * 
   * @return An array of doubles containing {@code dimension} values.
   * @throws IllegalStateException If the sequence limit has been reached.
   */
  private double[] nextVector() {
    if (this.count == this.nb) {
      throw new IllegalStateException("reached end of values");
    }

    final double[] v = new double[this.dimension];
    if (this.count == 0) {
      this.count++;
      return v;
    }

    // find the index c of the rightmost 0
    int c = 1;
    int value = this.count - 1;
    while ((value & 1) == 1) {
      value >>= 1;
      c++;
    }

    for (int i = 0; i < this.dimension; i++) {
      this.x[i] ^= this.direction[i][c];
      v[i] = this.x[i] / SCALE;
    }
    this.count++;
    return v;
  }

  @Override
  public Iterator<double[]> iterator() {
    return new Iterator<double[]>() {
      @Override
      public boolean hasNext() {
        return SobolSequenceGenerator.this.count < SobolSequenceGenerator.this.nb;
      }

      @Override
      public double[] next() {
        return SobolSequenceGenerator.this.nextVector();
      }
    };
  }
}
