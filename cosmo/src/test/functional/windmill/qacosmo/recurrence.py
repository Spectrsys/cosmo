# Generated by the windmill services transformer
from windmill.authoring import WindmillTestClient

def test():

    client = WindmillTestClient(__name__)

    client.click(id=u'viewNavCenterRight')
    client.click(id=u'viewNavCenterRight')
    client.wait(milliseconds=7000)
    client.doubleClick(id=u'hourDiv1-900')
    client.wait(milliseconds=7000)
    client.wait(milliseconds=7000)
    client.type(text=u'Testing Daily Recurrence', id=u'noteTitle')
    client.select(id=u'recurrenceInterval', option=u'Daily')
    client.type(text=u'A description of my test of Daily recurrenceInterval.', id=u'noteDescription')
    client.click(jsid=u'windmill.testingApp.cosmo.app.pim.layout.baseLayout.mainApp.rightSidebar.detailViewForm.buttonSection.saveButton.widgetId')
    client.wait(milliseconds=7000)
    client.wait(milliseconds=7000)
    client.doubleClick(id=u'hourDiv1-930')
    client.wait(milliseconds=7000)
    client.wait(milliseconds=7000)
    client.type(text=u'Testing Weekly Recurrence', id=u'noteTitle')
    client.select(id=u'recurrenceInterval', option=u'Weekly')
    client.type(text=u'A description of my test of weekly recurrenceInterval.', id=u'noteDescription')
    client.click(jsid=u'windmill.testingApp.cosmo.app.pim.layout.baseLayout.mainApp.rightSidebar.detailViewForm.buttonSection.saveButton.widgetId')
    client.wait(milliseconds=7000)
    client.wait(milliseconds=7000)
    client.doubleClick(id=u'hourDiv1-1000')
    client.wait(milliseconds=7000)
    client.wait(milliseconds=7000)
    client.type(text=u'Testing bi-weekly Recurrence', id=u'noteTitle')
    client.select(id=u'recurrenceInterval', option=u'Biweekly')
    client.type(text=u'A description of my test of bi-weekly recurrenceInterval.', id=u'noteDescription')
    client.click(jsid=u'windmill.testingApp.cosmo.app.pim.layout.baseLayout.mainApp.rightSidebar.detailViewForm.buttonSection.saveButton.widgetId')
    client.wait(milliseconds=7000)
    client.wait(milliseconds=7000)
    client.doubleClick(id=u'hourDiv1-1030')
    client.wait(milliseconds=7000)
    client.wait(milliseconds=7000)
    client.type(text=u'Testing monthly Recurrence', id=u'noteTitle')
    client.select(id=u'recurrenceInterval', option=u'Monthly')
    client.type(text=u'A description of my test of monthly recurrenceInterval.', id=u'noteDescription')
    client.click(jsid=u'windmill.testingApp.cosmo.app.pim.layout.baseLayout.mainApp.rightSidebar.detailViewForm.buttonSection.saveButton.widgetId')
    client.wait(milliseconds=7000)
    client.wait(milliseconds=7000)
    client.doubleClick(id=u'hourDiv1-1100')
    client.wait(milliseconds=7000)
    client.wait(milliseconds=7000)
    client.type(text=u'Testing yearly Recurrence', id=u'noteTitle')
    client.select(id=u'recurrenceInterval', option=u'Yearly')
    client.type(text=u'A description of my test of yearly recurrenceInterval.', id=u'noteDescription')
    client.click(jsid=u'windmill.testingApp.cosmo.app.pim.layout.baseLayout.mainApp.rightSidebar.detailViewForm.buttonSection.saveButton.widgetId')
    client.wait(milliseconds=3000)
    client.wait(milliseconds=3000)
    client.click(id=u'viewNavCenterRight')
    client.click(id=u'viewNavCenterRight')
    client.wait(milliseconds=7000)
    client.doubleClick(id=u'hourDiv1-900')
    client.wait(milliseconds=7000)
    client.wait(milliseconds=7000)
    client.type(text=u'Testing Daily Recurrence', id=u'noteTitle')
    client.select(id=u'recurrenceInterval', option=u'Daily')
    client.type(text=u'A description of my test of Daily recurrenceInterval.', id=u'noteDescription')
    client.click(jsid=u'windmill.testingApp.cosmo.app.pim.layout.baseLayout.mainApp.rightSidebar.detailViewForm.buttonSection.saveButton.widgetId')
    client.wait(milliseconds=7000)
    client.wait(milliseconds=7000)
    client.doubleClick(id=u'hourDiv1-930')
    client.wait(milliseconds=7000)
    client.wait(milliseconds=7000)
    client.type(text=u'Testing Weekly Recurrence', id=u'noteTitle')
    client.select(id=u'recurrenceInterval', option=u'Weekly')
    client.type(text=u'A description of my test of weekly recurrenceInterval.', id=u'noteDescription')
    client.click(jsid=u'windmill.testingApp.cosmo.app.pim.layout.baseLayout.mainApp.rightSidebar.detailViewForm.buttonSection.saveButton.widgetId')
    client.wait(milliseconds=7000)
    client.wait(milliseconds=7000)
    client.doubleClick(id=u'hourDiv1-1000')
    client.wait(milliseconds=7000)
    client.wait(milliseconds=7000)
    client.type(text=u'Testing bi-weekly Recurrence', id=u'noteTitle')
    client.select(id=u'recurrenceInterval', option=u'Biweekly')
    client.type(text=u'A description of my test of bi-weekly recurrenceInterval.', id=u'noteDescription')
    client.click(jsid=u'windmill.testingApp.cosmo.app.pim.layout.baseLayout.mainApp.rightSidebar.detailViewForm.buttonSection.saveButton.widgetId')
    client.wait(milliseconds=7000)
    client.wait(milliseconds=7000)
    client.doubleClick(id=u'hourDiv1-1030')
    client.wait(milliseconds=7000)
    client.wait(milliseconds=7000)
    client.type(text=u'Testing monthly Recurrence', id=u'noteTitle')
    client.select(id=u'recurrenceInterval', option=u'Monthly')
    client.type(text=u'A description of my test of monthly recurrenceInterval.', id=u'noteDescription')
    client.click(jsid=u'windmill.testingApp.cosmo.app.pim.layout.baseLayout.mainApp.rightSidebar.detailViewForm.buttonSection.saveButton.widgetId')
    client.wait(milliseconds=7000)
    client.wait(milliseconds=7000)
    client.doubleClick(id=u'hourDiv1-1100')
    client.wait(milliseconds=7000)
    client.wait(milliseconds=7000)
    client.type(text=u'Testing yearly Recurrence', id=u'noteTitle')
    client.select(id=u'recurrenceInterval', option=u'Yearly')
    client.type(text=u'A description of my test of yearly recurrenceInterval.', id=u'noteDescription')
    client.click(jsid=u'windmill.testingApp.cosmo.app.pim.layout.baseLayout.mainApp.rightSidebar.detailViewForm.buttonSection.saveButton.widgetId')
    client.wait(milliseconds=3000)
    client.wait(milliseconds=3000)
    client.wait(milliseconds=5000)
    client.click(link=u'Log out')