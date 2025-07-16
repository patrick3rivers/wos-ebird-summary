SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
java -cp "$SCRIPT_DIR/../jars/*" org._3rivers_ashtanga._2013.wos_ebird_summary.WosEbirdSummary $@
