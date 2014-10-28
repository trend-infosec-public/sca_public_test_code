#irc_client.pl
# A simple IRC robot.
# Usage: perl irc.pl
# Borrowed from O'Reilly Publishers
# http://oreilly.com/pub/h/1964
# Modified for use in Stonesoup
# by Gananand Kini

use strict;

# We will use a raw socket to connect to the IRC server.
use IO::Socket;

# The server to connect to and our details.
my $server = $ARGV[0];
my $serverport = $ARGV[1];
my $nick = $ARGV[2];
my $login = $ARGV[2];
#my $inputfile = $ARGV[3];

# The channel which the bot will join.
my $channel = "#StonesoupChannel";
#my $outputfile = "ssTestRes.txt";

# Connect to the IRC server.
my $sock = new IO::Socket::INET(PeerAddr => $server,
                                PeerPort => $serverport,
                                Proto => 'tcp') or
                                    die "Can't connect\n";

# Log on to the server.
print $sock "NICK $nick\r\n";
print $sock "USER $login 8 * :Stonesoup Robot\r\n";

# Read lines from the server until it tells us we have connected.
while (my $input = <$sock>) {
    # Check the numerical responses from the server.
    if ($input =~ /004/) {
        # We are now logged in.
        last;
    }
    elsif ($input =~ /433/) {
        die "Nickname is already in use.";
    }
}

# Join the channel.
print $sock "JOIN $channel\r\n";

# Feed the bot with input
my $not_done=1;
while ($not_done eq 1)
{
	my $inputline = <STDIN>;
	if($inputline =~ /^QUIT$/i){
		$not_done=0;
	}
	print $sock $inputline;
}

#open(OUTFILE,">$outputfile"); # open file to write to output

# Keep reading results from the server.
while (my $input = <$sock>) {
    chop $input;
    if ($input =~ /^PING(.*)$/i) {
        # We must respond to PINGs to avoid being disconnected.
        print $sock "PONG $1\r\n";
    }
    else {
        # Print the raw line received by the bot.
	if ($input =~ m/^:($nick)!~($nick)@(.*)$/i) {
        	#print OUTFILE "$input\n";
        	print "$input\n";
	}
    }
}	

exit 0
