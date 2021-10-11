import abc


class Message(abc.ABC):
    def __init__(self, sender):
        self.__sender = sender

    @property
    def sender(self):
        return self.__sender


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

    def __str__(self):
        return f'{{{self.sender.name};{self.criticality}}}'
