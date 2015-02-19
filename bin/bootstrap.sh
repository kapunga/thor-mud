#!/bin/sh

if [ -z "$THORMUD_HOME" ]
then
    echo "\$THORMUD_HOME is undefined."
    exit 0
fi

if [ -e "$THORMUD_HOME"/bootstrap ]
then
    echo "Bootstrapping database."
else
    echo "$THORMUD_HOME/bootstrap directory does not exist."
    exit 0
fi

neo4j-shell -c "CREATE CONSTRAINT ON (pantheon:Pantheon) ASSERT pantheon.tmId IS UNIQUE;"
neo4j-shell -c "CREATE CONSTRAINT ON (pantheon:Pantheon) ASSERT pantheon.name IS UNIQUE;"

neo4j-shell -c "LOAD CSV WITH HEADERS FROM 'file:$THORMUD_HOME/bootstrap/pantheons.csv' AS panth MERGE (p:Pantheon { tmId: toInt(panth.tmId), name: toString(panth.name), desc: toString(panth.desc) });"

neo4j-shell -c "CREATE CONSTRAINT ON (zone:Zone) ASSERT zone.tmId IS UNIQUE;"
neo4j-shell -c "CREATE CONSTRAINT ON (zone:Zone) ASSERT zone.name IS UNIQUE;"

neo4j-shell -c "LOAD CSV WITH HEADERS FROM 'file:$THORMUD_HOME/bootstrap/zones.csv' AS zone MERGE (z:Zone { tmId: toInt(zone.tmId), name: toString(zone.name) });"

neo4j-shell -c "LOAD CSV WITH HEADERS FROM 'file:$THORMUD_HOME/bootstrap/zones.csv' AS zone MATCH (p:Pantheon), (z:Zone) WHERE p.tmId = toInt(zone.panth) AND z.tmId = toInt(zone.tmId) MERGE (z)-[m:Member]->(p);"

DIRS=`ls -l "$THORMUD_HOME"/bootstrap | egrep '^d' | awk '{print $9}'`

for DIR in $DIRS
do
    if [ -e "$THORMUD_HOME"/bootstrap/$DIR/rooms.csv ]
    then
        neo4j-shell -c "LOAD CSV WITH HEADERS FROM 'file:$THORMUD_HOME/bootstrap/$DIR/rooms.csv' AS room MATCH (z:Zone) WHERE z.tmId = $DIR MERGE (r :Room { tmId: toInt(room.tmId), title: room.title, desc: room.desc })-[:Member]->(z);"
        if [ -e "$THORMUD_HOME"/bootstrap/$DIR/links.csv ]
        then
            neo4j-shell -c "LOAD CSV WITH HEADERS FROM 'file:$THORMUD_HOME/bootstrap/$DIR/links.csv' AS link MATCH (o:Room)-[:Member]->(z:Zone { tmId: $DIR })<-[:Member]-(d:Room) WHERE o.tmId = toInt(link.oId) AND d.tmId = toInt(link.dId) MERGE (o)-[l:Link { dir: link.dir }]->(d);"
        fi
    fi
done

neo4j-shell -c "MATCH (s:Soul), (p:Pantheon) WHERE s.name = '$CREATOR_NAME' AND p.tmId = -1 MERGE (s)-[:Spirit { level : 6 }]->(p) RETURN s, p;"
