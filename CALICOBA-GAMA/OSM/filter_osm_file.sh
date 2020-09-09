osmfilter $1 --keep="highway=primary =secondary =motorway =trunk =tertiary =residential =unclassified =living_street =road =motorway_link =trunk_link =primary_link =secondary_link =tertiary_link =service building=" --drop="highway=service and ( bus=yes or bus=designated )" > filtered_$1
qgis filtered_campus.osm

