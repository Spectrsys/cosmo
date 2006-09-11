#!/usr/bin/perl -w
# -*- Mode: Perl; indent-tabs-mode: nil; -*-

use strict;

use Cosmo::CMP ();
use Cosmo::DAV ();
use Cosmo::User ();
use File::Basename ();
use Getopt::Long ();

BEGIN { $0 = File::Basename::basename($0) }

use constant VERSION => '0.02';
use constant PROGRAM => $0;
use constant USAGE => <<EOT;
Usage: $0  [OPTIONS...] COMMAND

A script that is useful for testing how long it takes to do propfinds

Commands:
   createuser                            creates a new user
   populate [number of resources]        populates the collection with given number of resources
   propfind [depth]                      executes the propfind on the collection
    
Options:
  -s               server root URL (defaults to http://localhost:8080)
  -a               admin user (defaults to root)
  -w               admin password (defaults to cosmo)
  -u               username (defaults to test)
  -p               password (defaults to password)
  -c               collection within users's account.
  -d               print debugging information to STDOUT
  -h               list available command line options (this page)
  -v               print version information and exit

Report bugs to $0-cosmo\@osafoundation.org
EOT

use constant REQ_PROPFIND_ALLPROP => <<EOT;
<?xml version="1.0" encoding="utf-8" ?>
<D:propfind xmlns:D="DAV:">
<D:allprop/>
</D:propfind>
EOT

use constant REQ_MKCALENDAR => <<EOT;
<?xml version="1.0" encoding="utf-8" ?>
   <C:mkcalendar xmlns:D="DAV:"
                 xmlns:C="urn:ietf:params:xml:ns:caldav">
     <D:set>
       <D:prop>
         <D:displayname>Test Events</D:displayname>
         <C:calendar-description xml:lang="en"
   >Calendar restricted to events.</C:calendar-description>
         <C:supported-calendar-component-set>
           <C:comp name="VEVENT"/>
         </C:supported-calendar-component-set>
         <C:calendar-timezone><![CDATA[BEGIN:VCALENDAR
PRODID:-//Example Corp.//CalDAV Client//EN
VERSION:2.0
BEGIN:VTIMEZONE
TZID:US-Eastern
LAST-MODIFIED:19870101T000000Z
BEGIN:STANDARD
DTSTART:19671029T020000
RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10
TZOFFSETFROM:-0400
TZOFFSETTO:-0500
TZNAME:Eastern Standard Time (US & Canada)
END:STANDARD
BEGIN:DAYLIGHT
DTSTART:19870405T020000
RRULE:FREQ=YEARLY;BYDAY=1SU;BYMONTH=4
TZOFFSETFROM:-0500
TZOFFSETTO:-0400
TZNAME:Eastern Daylight Time (US & Canada)
END:DAYLIGHT
END:VTIMEZONE
END:VCALENDAR
]]></C:calendar-timezone>
</D:prop>
</D:set>
</C:mkcalendar>
EOT

use constant DEFAULT_SERVER_URL => 'http://localhost:8080';
use constant DEFAULT_ADMIN_USERNAME => 'root';
use constant DEFAULT_ADMIN_PASSWORD => 'cosmo';
use constant DEFAULT_USER_USERNAME => 'test';
use constant DEFAULT_USER_PASSWORD => 'password';
use constant DEFAULT_COLLECTION => 'collection';

use constant DEFAULT_NUM_RESOURCES => 3000;
use constant DEFAULT_DEPTH => 0;

$SIG{__DIE__} = sub { die sprintf("%s: %s", PROGRAM, $_[0]) };

my ($server_url, $admin_username, $admin_password,
    $opt_debug, $opt_help, $opt_version, $user_collection, $user_username, $user_password);

# process command line options
Getopt::Long::GetOptions(
    "s=s" => \$server_url,
    "u=s" => \$user_username,
    "p=s" => \$user_password,
    "a=s" => \$admin_username,
    "w=s" => \$admin_password,
    "d"   => \$opt_debug,
    "h"   => \$opt_help,
    "v"   => \$opt_version,
    "c=s" => \$user_collection
    );
(print USAGE and exit) if $opt_help;
(print sprintf("%s/%s\n", PROGRAM, VERSION) and exit) if $opt_version;

$server_url ||= DEFAULT_SERVER_URL;
chop($server_url) if $server_url =~ m|/$|;
$admin_username ||= DEFAULT_ADMIN_USERNAME;
$admin_password ||= DEFAULT_ADMIN_PASSWORD;
$user_username ||= DEFAULT_USER_USERNAME;
$user_password ||= DEFAULT_USER_PASSWORD;
$user_collection ||= DEFAULT_COLLECTION;


my $cmp = cmp_connect();
my $command = $ARGV[0];

if ($command eq 'createuser'){
    print "create user";
    my $user = create_user($cmp, $user_username, $user_password);
}

if ($command eq 'populate'){
   my $user = $cmp->get_user($user_username);
   $user->password($user_password);
   my $num_resources = $ARGV[1];
   my $dav = dav_connect($user);
   
   populate($dav, $user, $num_resources);
}

if ($command eq 'propfind'){
   my $user = $cmp->get_user($user_username);
   $user->password($user_password);
   my $dav = dav_connect($user);
   my $depth = $ARGV[1] ||= DEFAULT_DEPTH;
   propfind($dav, $user, $user_collection, $depth);
   
}

if ($command eq 'populate_caldata'){
   my $user = $cmp->get_user($user_username);
   $user->password($user_password);
   my $data_directory = $ARGV[1];
   my $dav = dav_connect($user);
   
   populate_caldata($dav, $user, $data_directory);
}

exit;

sub cmp_connect {
    my $cmp = Cosmo::CMP->new($server_url, $admin_username, $admin_password,
                              $opt_debug);
    $cmp->agent(PROGRAM . "/" . VERSION);

    $cmp->check_server_availability();
    return $cmp;
}

sub create_user {
    my $cmp = shift;
    my $username = shift;
    my $password = shift;
    
    my $user = Cosmo::User->new();
    $user->username($username);
    $user->password($password);
    $user->first_name($username);
    $user->last_name($username);
    $user->email("$username\@localhost");

    $cmp->create_user($user);
    print "Created account at " . $user->user_url() . "\n";

    return $user;
}

sub populate {
    my $dav = shift;
    my $user = shift;
    my $num_resources = shift;
    my $path_to_collection = path_to_collection($dav->server_url(), $user, $user_collection);
    my $useragent = $dav->dav->get_user_agent();

    print "\n";
    print "user: " . $user->username() . "\n";
    print "num: " . $num_resources . "\n" ;
    
    $dav->dav->delete($path_to_collection);
    $dav->dav->mkcol($path_to_collection);
    
        my $content = "content!";
    for (my $count = 0; $count < $num_resources; $count++) {
        print "count:  " . $count . "\n";
        my $request = HTTP::Request->new( "PUT", $path_to_collection . "/" . $count);
        $request->content($content);
        $useragent->request($request);
    }
}

sub populate_caldata {
    my $dav = shift;
    my $user = shift;
    my $data_directory = shift;
    
    my $path_to_collection = path_to_collection($dav->server_url(), $user, $user_collection);
    my $useragent = $dav->dav->get_user_agent();

    $dav->dav->delete($path_to_collection);
    my $request = HTTP::Request->new( "MKCALENDAR", $path_to_collection );
    $request->content(REQ_MKCALENDAR);
    $useragent->request($request);

    opendir(DIR, $data_directory);
    my @files = readdir(DIR);
    foreach my $file (@files) {
        open(FILE, $data_directory . "/" . $file);
        my $content ="";
        binmode FILE;
        while(<FILE>) { $content .= $_; }
        close FILE;
        
        my $request = HTTP::Request->new( "PUT", $path_to_collection . "/" . $file);
        $request->content($content);
        $request->header("Content-Type" => "text/calendar");
        $useragent->request($request);
        
        #print $content;
    }    
    
}

sub propfind{
    my $dav = shift;
    my $user = shift;
    my $collection = shift;
    my $path_to_collection = path_to_collection($dav->server_url(), $user, $collection);
    my $depth = shift;
    
    my $useragent = $dav->dav->get_user_agent();
    my $request = HTTP::Request->new( "PROPFIND", $path_to_collection );
    $request->content(REQ_PROPFIND_ALLPROP);
    $request->header("Depth" => $depthw);
    my $start = time();
    $useragent->request($request);
    my $end = time();
    my $elapsed = $end - $start;
    print "Time Elapsed: " . $elapsed . "\n";
    
}

sub propfind_slow{
    my $dav = shift;
    my $user = shift;
    my $collection = shift;
    my $path_to_collection = path_to_collection($dav->server_url(), $user, $collection);
    
    my $start = time();
    $dav->dav->propfind($path_to_collection,1);
    my $end = time();
    my $elapsed = $end - $start;
    print "Time Elapsed: " . $elapsed;
}

sub dav_connect {
    my $user = shift;

    my $dav = Cosmo::DAV->new($server_url, $user->username(),
                              $user->password(), $opt_debug);
    $dav->agent(PROGRAM . "/" . VERSION);

    $dav->check_server_availability();
    return $dav;
}

sub remove_user {
    my $cmp = shift;
    my $user = shift;

    $cmp->remove_user($user);
    print "Removed account at " . $user->user_url() . "\n";
}

sub path_to_collection {
  my $server_url = shift;
  my $user = shift;
  my $collection = shift;
  return $server_url . "/home" . "/" . $user->username() . "/" . $collection;
}
