
export JAMPAL_HOME=`dirname $scriptpath`
if [[ "$JAMPAL_SETUP" != YES ]] ; then
    PATH="$scriptpath:$PATH"
    OS=`uname -s`
    if [[ -f $HOME/.jampal/jampal.conf ]] ; then
        . $HOME/.jampal/jampal.conf
    elif [[ -f /etc/jampal/jampal.conf ]] ; then
        . /etc/jampal/jampal.conf
    else
      case $OS in 
        CYGWIN*)
          mkdir -p $HOME/.jampal
          cp $scriptpath/jampal.conf_cygwin $HOME/.jampal/jampal.conf
          echo -----------------------------------------------
          echo 'Please edit $HOME/.jampal/jampal.conf, update as necessary'
          echo -----------------------------------------------
          exit 2
          ;;
        Linux)
          mkdir -p $HOME/.jampal
          cp $scriptpath/jampal.conf_linux $HOME/.jampal/jampal.conf
          . $HOME/.jampal/jampal.conf
          ;;
        *)
          echo Unknown OS $OS
          exit 2
          ;;
       esac
    fi
    case $OS in 
      CYGWIN*)
        # Do not change this unless you have moved the looks files or have some others to include.
        export JAMPAL_CLASSPATH="$JAMPAL_HOME/jampal.jar:$JAMPAL_HOME/liquidlnf.jar:$JAMPAL_HOME/looks-2.3.0.jar\
:$JAMPAL_HOME/napkinlaf-1.2.jar:$JAMPAL_HOME/squareness.jar\
:$JAMPAL_HOME/InfoNodeilf-gpl.jar:$JAMPAL_HOME/lipstikLF-1.1.jar\
:$JAMPAL_HOME/nimrodlf.j16.jar:$JAMPAL_HOME/PgsLookAndFeel.jar:$FREETTS_HOME/lib/freetts.jar"
        JAMPAL_CLASSPATH=`cygpath -m -p "$JAMPAL_CLASSPATH"`
#        JAMPAL_CLASSPATH=`echo "$JAMPAL_CLASSPATH"|sed "s@\\\\\\\\@/@g"`
        if [[ ! -L "/Documents and Settings" || ! -L "/Users" ]] ; then
            echo "Please run setup_sygwin.sh with administrator access"
            exit 2
        fi
        ;;
      Linux)
        # This includes two possible places for LNF jars.
        export JAMPAL_CLASSPATH="$JAMPAL_HOME/jampal.jar:/usr/share/java/liquidlnf.jar\
:/usr/share/java/looks.jar:/usr/share/java/squareness.jar\
:$JAMPAL_HOME/liquidlnf.jar:$JAMPAL_HOME/looks-2.3.0.jar\
:$JAMPAL_HOME/napkinlaf-1.2.jar:$JAMPAL_HOME/squareness.jar\
:$JAMPAL_HOME/InfoNodeilf-gpl.jar:$JAMPAL_HOME/lipstikLF-1.1.jar\
:$JAMPAL_HOME/nimrodlf.j16.jar:$JAMPAL_HOME/PgsLookAndFeel.jar\
:$FREETTS_HOME/lib/freetts.jar"
        ;;
    esac
    # Do not change this
    export DELETEFRAME="ID3V2TAGTXXXjampal"
    export JAMPAL_SETUP=YES
fi