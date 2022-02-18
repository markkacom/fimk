/******************************************************************************
 * Copyright � 2014-2016 FIMK Developers                                      *
 ******************************************************************************/

Welcome to FIMK.

FIMK is open source software and based on NXT 
(https://bitbucket.org/JeanLucPicard/nxt/overview). 

This is FIMK version 0.6.0 which is based on NXT 1.5.10.

FIMK 0.6.0 is a mandatory update, you must update to this version before:

                    Jan 18th 2016 - 21.00 GMT

Running the FIMK software:

Dependencies: Java 8 or later needs to be installed first. Only the Oracle JVM
has been tested and supported.

There is no installation needed. Unpack the zip package to a directory of your choice
and it will be populated with the lompsa.exe client and fim server directory.

RUNNING THE SERVER:

Execute the run.sh script if using Linux,
or run.bat if using Windows. This will start a java server process, which will
begin logging its activities to the console. The initialization takes a few
seconds. When it is ready, you should see the message 
"FIM server 0.6.0 (based on NXT 1.5.10) started successfully". 

THROUGH THE WEB BROWSER:

Run the server first. Open a browser, without stopping the java process, and go to
http://localhost:7886 , where the FIMK UI should now be available. 

Warning: It is better to use only latin characters and no spaces in the path
to the fim installation directory, as the use of special characters may result
in permissions denied error in the browser, which is a known jetty issue.

Customization:

There are many configuration parameters that could be changed, but the defaults
are set so that normally you can run the program immediately after unpacking,
without any additional configuration. To see what options are there, open the
conf/fimk-default.properties file. All possible settings are listed, with
detailed explanation. If you decide to change any setting, do not edit
fimk-default.properties directly, but create a new conf/fimk.properties file
and only add to it the properties that need to be different from the default
values. You do not need to delete the defaults from fimk-default.properties, the
settings in fimk.properties override those in fimk-default.properties. This way,
when upgrading the software, you can safely overwrite fimk-default.properties
with the updated file from the new package, while your customizations remain
safe in the fimk.properties file.

Technical details:

The FIMK software is a client-server application. It consists of a java server
process, the one started by the run.sh script, and a javascript user interface
run in a browser. To run a node, forge, update the blockchain, interact with
peers, only the java process needs to be running, so you could logout and close
the browser but keep the java process running. If you want to keep forging, make
sure you do not click on "stop forging" when logging out. You can also just
close the browser without logging out.

The java process communicates with peers on port 7864 tcp by default. If you are
behind a router or a firewall and want to have your node accept incoming peer
connections, you should setup port forwarding. The server will still work though
even if only outgoing connections are allowed, so opening this port is optional.

The user interface is available on port 7886. This port also accepts http API
requests which other FIMK client applications could use.

The blockchain is stored on disk using the H2 embedded database, inside the
fim_db directory. When upgrading, you should not delete the old fim_db
directory, upgrades always include code that can upgrade old database files to
the new version whenever needed. But there is no harm if you do delete the
fim_db, except that it will take some extra time to download the blockchain
from scratch.

In addition to the default user interface at http://localhost:7886 , the
following urls are available:

http://localhost:7886/test - a list of all available http API requests, very
useful for client developers and for anyone who wants to execute commands
directly using the http interface without going through the browser UI.

http://localhost:7886/test?requestType=<specificRequestType> - same as above,
but only shows the form for the request type specified.
