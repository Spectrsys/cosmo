#!/usr/bin/env ruby
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

require 'preferences_helper'
require 'optparse'
require 'log4r'

include Log4r

server = "localhost"
port = "8080"
context = "/chandler"
username = "test"
password = "testtest"
key = "key"
value = "value"
action = "edit"
filename = nil

debug = false
  
OptionParser.new do |opts|
    opts.banner = "Usage: #{$0} [options]"

    opts.on("-s", "--server [SERVER]", "server address (default localhost)") do |s|
      server = s
    end
    
    opts.on("-c", "--context [CONTEXT]", "application context (default chandler)") do |c|
      context = c
    end
    
    opts.on("-p", "--port [PORT]", "server port (default 8080)") do |p|
      port = p
    end
    
    opts.on("-U", "--user [USER]", "username (default root)") do |u|
      username = u
    end
    
    opts.on("-P", "--password [PASSWORD]", "password (default cosmo)") do |p|
      password = p
    end
   
    opts.on("-e", "--edit [KEY=VAL]", "pref key and value to set (default key=value)") do |e|
      key, value = e.split("=")
      action = "edit"
    end
    
    opts.on("-d", "--delete [KEY]", "pref to delete") do |d|
      key = d
      action = "delete"
    end
    
     opts.on("-D", "--delete-all [KEY]", "pref to delete") do |d|
      key = d
      action = "deleteall"
    end
    
     opts.on("-l", "--list", "list prefs") do |l|
      action = "list"
    end
    
    opts.on("-f", "--file [filename]", "read from file") do |f|
      filename = f;
    end
    
    opts.on("-v", "--verbose", "enable verbose output") do |v|
      debug = true
    end
    
    # No argument, shows at tail.  This will print an options summary.
    opts.on_tail("-h", "--help", "Show this message") do
      puts opts
      exit
    end
    
end.parse!

helper = PreferencesHelper.new(server,port,context, username, password)
helper.set_debug(true) if debug

if(!filename.nil?)
  puts "reading from file #{filename}"
  File.foreach(filename) do |line|
    line =~ /^(deleteall|delete|set) (\S*) ?(.*)?/
    op = $1
    if(op=="deleteall")
      puts "deleteall #{$2}"
      helper.delete_prefs($2)
    elsif(op=="delete")
      puts "delete #{$2}"
      helper.delete_pref($2)
    elsif(op=="set")
      puts "set #{$2} = #{$3}"
      helper.set_pref($2,$3)
    end
  end
elsif(action=="edit")
  puts "failed" if(!helper.set_pref(key, value))
elsif(action=="delete")
  puts "failed" if(!helper.delete_pref(key))
elsif(action=="deleteall")
  puts "failed" if(!helper.delete_prefs(key))
elsif(action=="list")
  prefs = helper.get_prefs
  prefs.keys.sort.each do |key|
    puts "#{key}=#{prefs[key]}"
  end
end

puts "done!"
