# Generated by the windmill services transformer
from windmill.authoring import WindmillTestClient

def test():

    client = WindmillTestClient(__name__)

    client.wait(milliseconds=3000)
    client.wait(milliseconds=3000)
    client.wait(milliseconds=3000)
    client.click(link=u'Settings')
    client.wait(milliseconds=3000)
    client.type(text=u'steven@osafoundation.org', id=u'email')
    client.type(text=u'tester', id=u'password')
    client.type(text=u'testers', id=u'confirm')
    client.click(xpath=u'/html/body/div[4]/table/tbody/tr[2]/td[2]/div[5]/span/table/tbody/tr/td[3]/div/table/tbody/tr/td/input')
    client.type(text=u'tester', id=u'confirm')
    client.click(xpath=u'/html/body/div[4]/table/tbody/tr[2]/td[2]/div[5]/span/table/tbody/tr/td[3]/div/table/tbody/tr/td/input')
    client.wait(milliseconds=3000)
    client.click(link=u'Settings')
    client.click(xpath=u'/html/body/div[4]/table/tbody/tr[2]/td[2]/div[4]/span/div/table/tbody/tr/td[4]')
    client.check(id=u'showAccountBrowser')
    client.click(xpath=u'/html/body/div[4]/table/tbody/tr[2]/td[2]/div[4]/span/div/table/tbody/tr/td[6]')
    client.click(xpath=u'/html/body/div[4]/table/tbody/tr[2]/td[2]/div[5]/span/table/tbody/tr/td[3]/div/table/tbody/tr/td/input')
    client.wait(milliseconds=3000)
    client.click(link=u'Settings')
    client.wait(milliseconds=3000)
    client.click(xpath=u'/html/body/div[4]/table/tbody/tr[2]/td[2]/div[4]/span/div/table/tbody/tr/td[4]')
    client.check(id=u'showAccountBrowser')
    client.wait(milliseconds=3000)
    client.click(xpath=u'/html/body/div[4]/table/tbody/tr[2]/td[2]/div[5]/span/table/tbody/tr/td[3]/div/table/tbody/tr/td/input')
    client.wait(milliseconds=3000)
    client.click(xpath=u'/html/body/div[2]/form/div/div/span/div[2]/div[3]/img')
    client.select(xpath=u'/html/body/div[4]/table/tbody/tr[2]/td[2]/div[4]/div/table/tbody/tr[3]/td[2]/div/select', option=u'Apple iCal v2.x')
    client.wait(milliseconds=2000)
    client.select(xpath=u'/html/body/div[4]/table/tbody/tr[2]/td[2]/div[4]/div/table/tbody/tr[3]/td[2]/div/select', option=u'Feed Reader')
    client.wait(milliseconds=2000)
    client.click(xpath=u'/html/body/div[4]/table/tbody/tr[2]/td[2]/div[5]/span/table/tbody/tr/td[3]/div/table/tbody/tr/td[3]/input')
    client.wait(milliseconds=3000)
    client.wait(milliseconds=3000)