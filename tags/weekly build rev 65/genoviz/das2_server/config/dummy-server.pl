#!/usr/bin/env perl

# $Id: dummy-server.pl 3182 2007-12-12 02:39:26Z chervitz $
#
# A perl script to simulate an indefinitely running process.
# Generates one line of output per second.
# Used as a stand-in for a DAS server for testing the das2launch script.

use strict;

select(STDOUT); $|=1;
my $cnt=0;

while(1) {
    $cnt++;
    sleep 1;
    if ($cnt % 2 == 0) {
        print "Hello faddah.\n";
        warn "warn: Hello faddah.\n";
    } else {
        print "Hello muddah.\n";
        warn "warn: Hello muddah.\n";
    }
## Uncomment this to simulate server crash:
#    if ($cnt == 10) {
#        warn "fatal: something bad happened\n";
#        exit $cnt;
#    }

}

exit;
