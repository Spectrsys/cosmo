require "cosmo/atom_client"

class String
  def starts_with?(chars)
    self.match(/^#{chars}/) ? true : false
  end
end

class PreferencesHelper

  def initialize(server, port, context, user, pass)
      @atomClient = Cosmo::AtomClient.new(server,port,context, user, pass)
  end
  
  def set_debug(debug)
    if(debug==true)
      Logger['AtomClient'].outputters = Outputter.stdout
      Logger['AtomClient'].level = Log4r::DEBUG
    else
      Logger['AtomClient'].outputters = nil
    end
  end
  
  def set_pref(key, value)
    resp = @atomClient.createPreference(key, value)
    if(resp.code==409)
      resp = @atomClient.updatePreference(key, value)
      return false if(resp.code!=200)
    else
      return false if(resp.code!=201)
    end
    return true
  end
  
  def delete_pref(key)
    resp = @atomClient.deletePreference(key)
    return (resp.code==204)
  end
  
  def delete_prefs(key)
    get_prefs().each do |k, v|
      delete_pref(k) if k.starts_with?(key)
    end
    return true
  end
  
  def get_prefs
    resp = @atomClient.getPreferencesFeed
    if(resp.code==200)
      keys = resp.data.scan(/\"key\">(.*)</)
      values = resp.data.scan(/\"value\">(.*)</)
      prefs = {}
      for i in (0..keys.length)
        prefs[keys[i][0]] = values[i][0] if(!keys[i].nil? && !values[i].nil?)
      end
      return prefs
    else
      return nil
    end
  end
  
end