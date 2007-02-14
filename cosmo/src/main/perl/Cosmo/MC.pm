#!/usr/bin/perl -w
# -*- Mode: Perl; indent-tabs-mode: nil; -*-
# 
# Copyright 2007 Open Source Applications Foundation
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#     http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

package Cosmo::MC;

use Cosmo::ClientBase ();

use strict;
use base qw(Cosmo::ClientBase);

sub new {
    my $class = shift;
    my $self = Cosmo::ClientBase->new(@_);
    return bless $self, $class;
}

sub publish {
}

sub update {
}

sub subscribe {
    my $self = shift;
    my $uid = shift;
    my $parentUid = shift;

    my $url = $self->collection_url($uid);
    $url = sprintf("%s?parent=%s", $url, $parentUid) if $parentUid;

    my $req = HTTP::Request->new(GET => $url);
    print $req->as_string . "\n" if $self->{debug};

    my $res = $self->request($req);
    print $res->as_string . "\n" if $self->{debug};

    if (! $res->is_success) {
        die "Bad username or password\n" if $res->code == 401;
        die "Collection $uid does not exist\n" if $res->code == 404;
        die "UID $uid is already in use\n" if $res->code == 409;
        die "Parent item is not a collection\n" if $res->code == 412;
        die $res->status_line . "\n";
    }

    if (! $res->code == 201) {
        warn "Success code " . $res->code . " not recognized\n";
    }

    # XXX return collection
}

sub synchronize {
}

sub delete {
}

sub mc_url {
    my $self = shift;

    return sprintf("%s/mc", $self->server_url);
}

sub collection_url {
    my $self = shift;
    my $uid = shift;

    return sprintf("%s/collection/%s", $self->mc_url, $uid);
}

1;
