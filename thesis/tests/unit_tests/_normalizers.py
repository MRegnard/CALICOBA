import unittest

import calicoba


class AllTimeAbsoluteNormalizerTestCase(unittest.TestCase):
    def test_1_value(self):
        n = calicoba.agents.AllTimeAbsoluteNormalizer()
        self.assertEqual(1, n(2))

    def test_2_values(self):
        n = calicoba.agents.AllTimeAbsoluteNormalizer()
        self.assertEqual(1, n(2))
        self.assertEqual(0.5, n(1))

    def test_1_negative_value(self):
        n = calicoba.agents.AllTimeAbsoluteNormalizer()
        self.assertEqual(-1, n(-2))

    def test_2_negative_values(self):
        n = calicoba.agents.AllTimeAbsoluteNormalizer()
        self.assertEqual(-1, n(-2))
        self.assertEqual(-0.5, n(-1))
