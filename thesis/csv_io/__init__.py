import abc
import pathlib
import re
import typing as typ

import lark


class CSVIO(abc.ABC):
    def __init__(self, file_name: pathlib.Path, mode: str, encoding: str = 'utf8', separator: str = ',',
                 line_comment: str = '#', strip: bool = True, disable_escape_char: bool = False,
                 allow_columns_nb_mismatch: bool = False):
        if line_comment == '"':
            raise ValueError(f'illegal line comment character {line_comment}')
        if len(separator) != 1 or (separator == '"' and not disable_escape_char):
            raise ValueError(f'illegal separator character {separator}')
        if separator == line_comment:
            raise ValueError('separator and line comment characters cannot be identical')
        # noinspection PyTypeChecker
        self._file = file_name.open(mode=mode, encoding=encoding)
        self._separator = separator
        self._line_comment = line_comment
        self._strip = strip
        self._disable_escape = disable_escape_char
        self._allow_columns_nb_mismatch = allow_columns_nb_mismatch
        self._columns = []
        self._default_values = {}

    @property
    def column_names(self) -> typ.List[str]:
        return list(self._columns)

    def set_column_default(self, column: str, default):
        if column not in self._columns:
            raise KeyError(f'undefined column {column}')
        self._default_values[column] = default

    def get_column_default(self, column: str):
        return self._default_values.get(column, default=None)

    def close(self):
        self._file.close()

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()
        return False

    def __del__(self):
        self.close()


class CSVWriter(CSVIO):
    """Wrapper that makes it easier to write CSV files."""

    def __init__(self, file_name: pathlib.Path, *columns: str, append: bool = False, write_header: bool = True,
                 **kwargs):
        super().__init__(file_name, mode='a' if append else 'w', **kwargs)
        self._columns = columns
        if not append and write_header:
            self._file.write(self._separator.join(self._sanitize_data(c) for c in self._columns) + '\n')

    def write_line(self, **data):
        self._file.write(self._separator.join(self._format_line(**data)))

    def write_lines(self, *lines: typ.Dict[str, typ.Any]):
        self._file.writelines(self._format_line(**data) for data in lines)

    def write_comment(self, message: str):
        self._file.write(f'# {message}\n')

    def _format_line(self, **data) -> str:
        columns_nb = len(self._columns)
        values_nb = len(data)
        if not self._allow_columns_nb_mismatch and values_nb != columns_nb:
            raise ValueError(f'extra values detected: expected {columns_nb}, got {values_nb}')
        line = ['' for _ in range(values_nb)]
        for c, v in data.items():
            if c not in self._columns:
                raise ValueError(f'undefined column {c}')
            line[self._columns.index(c)] = self._sanitize_data(str(v)) if v is not None else ''
        return self._separator.join(line) + '\n'

    def _sanitize_data(self, s: str) -> str:
        if self._strip:
            s = s.strip()
        if not self._disable_escape:
            if '"' in s:
                s = s.replace('"', r'""')
                return f'"{s}"'
            elif self._separator in s:
                return f'"{s}"'
        return s

    def flush(self):
        self._file.flush()


class CSVReader(CSVIO):
    """Wrapper that makes it easier to read CSV files."""

    def __init__(self, file_name: pathlib.Path, **kwargs):
        super().__init__(file_name, mode='r', **kwargs)
        self._columns = self._parse_line(self._file.readline())
        ws = set(' \t')
        if self._separator in ws:
            ws.remove(self._separator)
        self._spaces = '[' + ''.join(ws) + ']'
        self._parser = lark.Lark(fr"""
        line: value ("{self._separator}" value)*
        ?value: VALUE
              | QUOTED_VALUE -> quoted_value
              | empty
        empty:
        VALUE: /[^"{re.escape(self._separator)}]+/
        QUOTED_VALUE: /{self._spaces}*"([^"]|"")*"{self._spaces}*/
        """.strip(), start='line')

    def read_line(self, missing_column_prefix: str = '_c') -> typ.Optional[typ.Dict[str, str]]:
        values = None
        while values is None:
            line = self._file.readline()
            if not line:
                return None
            values = self._parse_line(line)
        return self._line_to_dict(*values, missing_column_prefix)

    def read_lines(self, missing_column_prefix: str = '_c') -> typ.Generator[typ.Dict[str, str]]:
        # TODO check if offset needed
        for line in self._file.readlines()[1:]:
            if (values := self._parse_line(line)) is not None:
                yield self._line_to_dict(*values, missing_column_prefix)

    def _parse_line(self, line: str) -> typ.Optional[typ.List[str]]:
        if line[-1] in '\r\n':
            line = line[:-1]
        elif line[-2:] == '\r\n':
            line = line[:-2]

        if re.match('^' + self._spaces + '*' + re.escape(self._line_comment), line):
            return None

        if self._disable_escape:
            return [(s.strip() if self._strip else s) or None for s in line.split(self._separator)]
        else:
            return _Transformer().transform(self._parser.parse(line))

    def _line_to_dict(self, *values: str, missing_column_prefix: str = '_c') -> typ.Dict[str, str]:
        columns_nb = len(self._columns)
        values_nb = len(values)
        if not self._allow_columns_nb_mismatch and values_nb != columns_nb:
            raise ValueError(f'extra values detected: expected {columns_nb}, got {values_nb}) in\n{values}')
        data = {}
        for i in range(max(values_nb, columns_nb)):
            c = self._columns[i] if i < columns_nb else f'{missing_column_prefix}{i}'
            v = values[i] if i < values_nb and values[i] is not None else self.get_column_default(c)
            data[c] = v
        return data


class _Transformer(lark.Transformer):
    # noinspection PyMethodMayBeStatic
    def line(self, items: typ.List[lark.Token]):
        return [str(item) for item in items]

    # noinspection PyMethodMayBeStatic
    def quoted_value(self, s: typ.List[lark.Token]):
        return s[0].strip()[1:-1].replace('""', '"')

    # noinspection PyMethodMayBeStatic
    def empty(self, _):
        return None
