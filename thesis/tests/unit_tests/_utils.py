import unittest

from cobopti import utils


class GetXCTestCase(unittest.TestCase):
    def test_positive(self):
        a = 1, 1
        e = 3, 4
        yc = 10
        self.assertEqual(7, utils.get_xc(a, e, yc))

    def test_negative(self):
        a = 3, 1
        e = 0, 4
        yc = 9
        self.assertEqual(-5, utils.get_xc(a, e, yc))
