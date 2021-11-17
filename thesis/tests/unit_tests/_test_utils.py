import unittest

import test_utils


class SobolSequenceTestCase(unittest.TestCase):
    def test_1d_points(self):
        expected_points = [(0,), (1 / 2,), (3 / 4,), (1 / 4,), (3 / 8,), (7 / 8,), (5 / 8,), (1 / 8,), ]
        for ep, p in zip(expected_points, test_utils.SobolSequence(1, len(expected_points))):
            self.assertEqual(ep, p)

    def test_2d_points(self):
        expected_points = [(0, 0), (1 / 2, 1 / 2), (3 / 4, 1 / 4), (1 / 4, 3 / 4), (3 / 8, 3 / 8), (7 / 8, 7 / 8),
                           (5 / 8, 1 / 8), (1 / 8, 5 / 8), ]
        for ep, p in zip(expected_points, test_utils.SobolSequence(2, len(expected_points))):
            self.assertEqual(ep, p)


class PeriodDetectorTestCase(unittest.TestCase):
    def setUp(self):
        self.detector = test_utils.PeriodDetector(10)

    def test_not_full(self):
        self.assertFalse(self.detector.is_full)

    def test_full(self):
        for i in range(10):
            self.detector.append(i)
        self.assertTrue(self.detector.is_full)

    def test_append_not_full(self):
        self.assertEqual(0, len(self.detector.values()))
        self.detector.append(1)
        self.assertEqual(1, len(self.detector.values()))
        self.assertIn(1, self.detector.values())

    def test_append_full(self):
        values = list(range(10))
        for i in values:
            self.detector.append(i)
        self.assertEqual(values, self.detector.values())
        self.detector.append(-1)
        self.assertEqual(values[1:] + [-1], self.detector.values())

    def test_has_converged_2(self):
        for i in [1, 2, 1, 2, 1, 2, 1, 2, 1, 2]:
            self.detector.append(i)
        self.assertTrue(self.detector.has_converged)

    def test_has_converged_3(self):
        for i in [1, 2, 3, 1, 2, 3, 1, 2, 3]:
            self.detector.append(i)
        self.assertTrue(self.detector.has_converged)

    def test_get_period_distances_2(self):
        for i in [1, 2, 1, 2, 1, 2, 1, 2, 1, 2]:
            self.detector.append(i)
        self.assertEqual(((0, 2),), self.detector.get_period_distances())

    def test_get_period_distances_3(self):
        for i in [1, 2, 3, 1, 2, 3, 1, 2, 3]:
            self.detector.append(i)
        self.assertEqual(((0, 3),), self.detector.get_period_distances())
