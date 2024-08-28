#! /bin/bash
basedir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo ${basedir}
[ -d ${basedir}/../target/failsafe-reports/ ] || mkdir -p ${basedir}/../target/failsafe-reports/
cp ${basedir}/../../scripts/TEST-failsafe-summary.txt  ${basedir}/../target/failsafe-reports/


