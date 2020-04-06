# Original code: Joe Kington
# https://stackoverflow.com/questions/31410043/hiding-lines-after-showing-a-pyplot-figure
import json

import matplotlib.pyplot as plt
import numpy as np


def interactive_legend(ax=None):
    if ax is None:
        ax = plt.gca()
    if ax.legend_ is None:
        ax.legend()

    return InteractiveLegend(ax.get_legend())


class InteractiveLegend:
    def __init__(self, legend):
        self.legend = legend
        self.fig = legend.axes.figure

        self.lookup_artist, self.lookup_handle = self._build_lookups(legend)
        self._setup_connections()

        self.update()

    def _setup_connections(self):
        for artist in self.legend.texts + self.legend.legendHandles:
            artist.set_picker(10) # 10 points tolerance

        self.fig.canvas.mpl_connect('pick_event', self.on_pick)
        self.fig.canvas.mpl_connect('button_press_event', self.on_click)

    def _build_lookups(self, legend):
        labels = [t.get_text() for t in legend.texts]
        handles = legend.legendHandles
        label2handle = dict(zip(labels, handles))
        handle2text = dict(zip(handles, legend.texts))

        lookup_artist = {}
        lookup_handle = {}
        for artist in legend.axes.get_children():
            if artist.get_label() in labels:
                handle = label2handle[artist.get_label()]
                lookup_handle[artist] = handle
                lookup_artist[handle] = artist
                lookup_artist[handle2text[handle]] = artist

        lookup_handle.update(zip(handles, handles))
        lookup_handle.update(zip(legend.texts, handles))

        return lookup_artist, lookup_handle

    def on_pick(self, event):
        handle = event.artist
        if handle in self.lookup_artist:

            artist = self.lookup_artist[handle]
            artist.set_visible(not artist.get_visible())
            self.update()

    def on_click(self, event):
        if event.button == 3:
            visible = False
        elif event.button == 2:
            visible = True
        else:
            return

        for artist in self.lookup_artist.values():
            artist.set_visible(visible)
        self.update()

    def update(self):
        for artist in self.lookup_artist.values():
            handle = self.lookup_handle[artist]
            if artist.get_visible():
                handle.set_visible(True)
            else:
                handle.set_visible(False)
        self.fig.canvas.draw()

    def show(self):
        plt.show()


def load_file_data(filename: str):
    timestamps = []
    nodes_data = {}

    with open(filename) as f:
        contents = json.load(f)['data']
        for item in contents:
            timestamps.append(item['timestamp'])
            for node in item['nodes']:
                key = node['location']
                if key not in nodes_data:
                    nodes_data[key] = []
                nodes_data[key].append(node['people_nb'])

    return timestamps, nodes_data


def load_data(ax):
    timestamps1, nodes_data1 = load_file_data('../../output/UT3_directions/test1.json')
    timestamps2, nodes_data2 = load_file_data('../../output/UT3_directions/test2.json')
    for node_name, node_data in nodes_data1.items():
        ax.plot(timestamps1, node_data, label=node_name + ' - 1')
    for node_name, node_data in nodes_data2.items():
        ax.plot(timestamps2, node_data, label=node_name + ' - 2')
    # TODO ajouter une légende aux axes


if __name__ == '__main__':
    fig, ax = plt.subplots()
    load_data(ax)

    ax.legend(loc='upper left', bbox_to_anchor=(1.05, 1), ncol=2, borderaxespad=0)
    fig.subplots_adjust(right=0.55)
    fig.suptitle('Right-click to hide all\nMiddle-click to show all', va='top', size='large')

    leg = interactive_legend()
    plt.show()







































