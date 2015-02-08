#!/bin/sh

################################################################################################
# This script sets up a remote instance of ThorMud and starts it up.                           #
# $THORMUD_HOME should be set to the root directory of the THORMUD_PROJECT                     #
# $THORMUD_HOST should be set to the host username and hostname where ThorMud is being hosted. #
# This sets up everything in the ~/ directory on the remote host.                              #
################################################################################################

ssh $THORMUD_HOST << EOF
mkdir ~/ThorMud
mkdir ~/ThorMud/bin
mkdir ~/ThorMud/lib
mkdir ~/ThorMud/log
EOF

scp "$THORMUD_HOME"/bin/thormud $THORMUD_HOST:~/ThorMud/bin/
scp "$THORMUD_HOME"/bin/purge.sh $THORMUD_HOST:~/ThorMud/bin/
scp "$THORMUD_HOME"/bin/bootstrap.sh $THORMUD_HOST:~/ThorMud/bin/
scp -r "$THORMUD_HOME"/bootstrap $THORMUD_HOST:~/ThorMud/
scp "$THORMUD_HOME"/target/scala-2.11/Thor*-assembly-*.jar kapunga@kapunga.org:~/ThorMud/lib/ThorMud.jar

ssh $THORMUD_HOST << EOF
chmod -R 755 ~/ThorMud/bin
~/ThorMud/bin/thormud start
EOF
