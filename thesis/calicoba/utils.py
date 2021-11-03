import typing as typ

Point = typ.Tuple[float, float]


def get_xc(a: Point, e: Point, yc: float) -> float:
    """Return the x component of point C (xc), relative to point A = (xa, ya),
    of a 2D ABC rectangle triangle of hypothenus AC, whose yc and point A are know
    as well as a point E = (xe, ye) on the AC side paired with another point D of same y component on the AB side.
    As such, sides DE and BC are parallel and we can use Thales’s theorem to compute xc.

    :param a: Point A = (xa, ya).
    :param e: Point E = (xe, ye) located on side AC.
    :param yc: Y component of point C.
    :return: The x component of point C (xc).
    """
    xa, ya = a
    xe, ye = e
    return xa + (yc - ya) * (xe - xa) / (ye - ya)
