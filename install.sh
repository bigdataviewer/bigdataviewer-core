#!/bin/bash

# This script is shamelessly adapted from https://github.com/saalfeldlab/n5-utils, thanks @axtimwalde & co!

VERSION="10.3.2-SNAPSHOT"
INSTALL_DIR=${1:-$(pwd)}

echo ""
echo "Installing into $INSTALL_DIR"

# check for operating system
if [[ "$OSTYPE" == "linux-gnu" ]]; then
  echo "Assuming on Linux operating system"
  MEM=$(cat /proc/meminfo | grep MemTotal | sed s/^MemTotal:\\\s*\\\|\\\s\\+[^\\\s]*$//g)
  MEMGB=$(($MEM/1024/1024))
  MEM=$((($MEMGB/5)*4))
elif [[ "$OSTYPE" == "darwin"* ]]; then
  echo "Assuming on MacOS X operating system"
  # sysctl returns total hardware memory size in bytes
  MEM=$(sysctl hw.memsize | grep hw.memsize | sed s/hw.memsize://g)
  MEMGB=$(($MEM/1024/1024/1024))
  MEM=$((($MEMGB/5)*4))
else
  echo "ERROR - Operating system (arg2) must be either linux or osx - EXITING (on windows please run as a normal Java class from e.g. Eclipse)"
  exit
fi

echo "Available memory:" $MEMGB "GB, setting Java memory limit to" $MEM "GB"

mvn clean install
mvn -Dmdep.outputFile=cp.txt -Dmdep.includeScope=runtime dependency:build-classpath

echo ""
echo "Installing 'bdv' command into" $INSTALL_DIR

echo '#!/bin/bash' > bdv
echo '' >> bdv
echo "JAR=\$HOME/.m2/repository/sc/fiji/bigdataviewer-core/${VERSION}/bigdataviewer-core-${VERSION}.jar" >> bdv
echo 'java \' >> bdv
echo "  -Xmx${MEM}g \\" >> bdv
echo '  -XX:+UseConcMarkSweepGC \' >> bdv
echo -n '  -cp $JAR:' >> bdv
echo -n $(cat cp.txt) >> bdv
echo ' \' >> bdv
echo '  bdv.BigDataViewer "$@"' >> bdv


chmod a+x bdv

if [ $(pwd) == "$INSTALL_DIR" ]; then
    echo "Installation directory equals current directory, we are done."
else
	echo "Creating directory $INSTALL_DIR and moving files..."
    mkdir -p $INSTALL_DIR
    mv bdv $INSTALL_DIR/
fi

rm cp.txt

echo "Installation finished."
