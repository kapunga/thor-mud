#!/bin/sh

################################################################################################
# This script updates a running remote ThorMud instance.                                       #
# $THORMUD_HOME should be set to the root directory of the THORMUD_PROJECT                     #
# $THORMUD_HOST should be set to the host username and hostname where ThorMud is being hosted. #
# This presumes the directory structure remotely was set up as ~/ThorMud.                      #
################################################################################################

if [ -z "$THORMUD_HOME" ]
then
    echo "\$THORMUD_HOME is undefined."
    exit 0
fi

if [ -z "$THORMUD_HOST" ]
then
    echo "\$THORMUD_HOST is undefined."
    exit 0
fi

LOCATION=`pwd`

cd "$THORMUD_HOME"
echo "Stopping remote ThorMud instance..."
ssh $THORMUD_HOST << EOF
~/ThorMud/bin/thormud stop
rm ~/ThorMud/lib/ThorMud.jar
rm -r ~/ThorMud/bootstrap
EOF
scp "$THORMUD_HOME"/target/scala-2.11/Thor*-assembly-*.jar $THORMUD_HOST:~/ThorMud/lib/ThorMud.jar
scp -r "$THORMUD_HOME"/bootstrap $THORMUD_HOST:~/ThorMud/
ssh $THORMUD_HOST << EOF
~/ThorMud/bin/thormud start
EOF

cd "$LOCATION"
