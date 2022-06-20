import random
import typing as typ

Point = typ.Tuple[float, float]


def get_xc(top_point: Point, intermediate_point: Point, yc: float) -> float:
    """Returns the x component of point C (xc), relative to point A = (xa, ya),
    of a 2D ABC rectangle triangle of hypothenus AC, whose yc and point A are know
    as well as a point E = (xe, ye) on the AC side paired with another point D of same y component on the AB side.
    As such, sides DE and BC are parallel and we can use Thales’s theorem to compute xc.

    :param top_point: Point A = (xa, ya), at the base of the rectangle triangle.
    :param intermediate_point: Point E = (xe, ye) located on the hypothenus (side AC).
    :param yc: Y component of point C, at the lower point of the rectangle triangle.
    :return: The x component of point C (xc).
    """
    xa, ya = top_point
    xe, ye = intermediate_point
    return xa + (yc - ya) * (xe - xa) / (ye - ya)


def randbool():
    return random.random() < 0.5
