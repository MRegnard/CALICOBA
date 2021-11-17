import abc


class Message(abc.ABC):
    def __init__(self, sender):
        self.__sender = sender

    @property
    def sender(self):
        return self.__sender

    def __repr__(self):
        return f'{self.__class__.__name__}{{sender="{self.sender}"}}'


class CriticalityMessage(Message):
    def __init__(self, sender, criticality: float, variation_direction: int):
        super().__init__(sender)
        self.__crit = criticality
        self.__var_dir = variation_direction

    @property
    def criticality(self) -> float:
        return self.__crit

    @property
    def variation_direction(self) -> int:
        return self.__var_dir

    def __repr__(self):
        return f'{self.__class__.__name__}{{sender="{self.sender}", criticality={self.criticality}}}'


class PointMessage(Message):
    pass


class VariationSuggestionMessage(PointMessage):
    def __init__(self, sender, direction: int, steps_number: int):
        super().__init__(sender)
        self.__direction = direction
        self.__steps_number = steps_number

    @property
    def direction(self) -> int:
        return self.__direction

    @property
    def steps_number(self) -> int:
        return self.__steps_number

    def __repr__(self):
        return f'{self.__class__.__name__}{{sender="{self.sender}", direction={self.direction},' \
               f' steps number={self.steps_number}}}'


class NewValueSuggestionMessage(PointMessage):
    def __init__(self, sender, new_parameter_value: float, climbing: bool, expected_criticality: int):
        super().__init__(sender)
        self.__new_value = new_parameter_value
        self.__climbing = climbing
        self.__expected_crit = expected_criticality

    @property
    def new_parameter_value(self) -> float:
        return self.__new_value

    @property
    def climbing(self) -> bool:
        return self.__climbing

    @property
    def expected_criticality(self) -> int:
        return self.__expected_crit

    def __repr__(self):
        return f'{self.__class__.__name__}{{sender="{self.sender}", new parameter value={self.__new_value}, ' \
               f'climbing={self.__climbing}, expected criticality={self.expected_criticality}}}'


class RequestOtherSideMessage(Message):
    pass


class NewSlopeFoundMessage(Message):
    pass
