#!/bin/sh

neo4j-shell -c "MATCH (r:Room)-[l]-() DELETE l;"
neo4j-shell -c "MATCH (p:Pantheon)-[l]-() DELETE l;"
neo4j-shell -c "MATCH (z:Zone)-[l]-() DELETE l;"
neo4j-shell -c "MATCH (r:Room), (p:Pantheon), (z:Zone) DELETE r, p, z;"
