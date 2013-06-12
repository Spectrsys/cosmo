<?php

echo '<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="
        http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-2.0.xsd
    ">

    <changeSet author="lukasz" id="2013-06-12">
';

$num = 51;
$name = 'user';
$pass = 'vatalend2n';

for ($i = 1; $i < $num; $i++) {
   $uname = $name . $i;
   $password = md5($pass . $i);
   $uuidUser = '265cf78c-fce8-4137-a3c0-fd7adb6a53d6' . $i;

   echo "
        <insert tableName=\"USERS\">
            <column name=\"CREATEDATE\" value=\"1370894964077\" />
            <column name=\"ETAG\" value=\"5f1KODsKk4P+rFFC8+6U4zOzU2A=\" />
            <column name=\"MODIFYDATE\" value=\"1370894964077\" />
            <column name=\"ADMIN\" valueBoolean=\"true\" />
            <column name=\"EMAIL\" value=\"$uname@example.com\" />
            <column name=\"FIRSTNAME\" value=\"User $i\" />
            <column name=\"LASTNAME\" value=\"User $i\" />
            <column name=\"LOCKED\" valueBoolean=\"false\" />
            <column name=\"PASSWORD\" value=\"$password\"/><!-- $pass$i -->
            <column name=\"UID\" value=\"$uuidUser\" />
            <column name=\"USERNAME\" value=\"$uname\" />
        </insert>";


   $uuidHomeCollection = '9d09dc70-d12a-452b-b0ea-1b46bcbcaad' . $i;

echo "
        <insert tableName=\"ITEM\">
            <column name=\"ITEMTYPE\" value=\"homecollection\" />
            <column name=\"CREATEDATE\" value=\"1370894964077\" />
            <column name=\"MODIFYDATE\" value=\"1370894964077\" />
            <column name=\"ETAG\" value=\"U9FdHd5nMd+BOuGBqZ4OLMM2Vek=\" />
            <column name=\"DISPLAYNAME\" value=\"$uname\" />
            <column name=\"ITEMNAME\" value=\"$uname\" />
            <column name=\"UID\" value=\"$uuidHomeCollection\" />
            <column name=\"OWNERID\" valueComputed=\"SELECT ID FROM USERS WHERE USERNAME='$uname'\" />
            <column name=\"VERSION\" value=\"0\" />
        </insert>";

    $uuidCollection = 'dbac13a6-10c1-44db-92b3-a47c5b29be1' . $i;

echo "
        <insert tableName=\"ITEM\">
            <column name=\"ITEMTYPE\" value=\"collection\" />
            <column name=\"CREATEDATE\" value=\"1370894964077\" />
            <column name=\"MODIFYDATE\" value=\"1370894964077\" />
            <column name=\"ETAG\" value=\"nUAhhdsfEFyzEExx2Prsy3bEoVc=\" />
            <column name=\"DISPLAYNAME\" value=\"$uname items\" />
            <column name=\"ITEMNAME\" value=\"$uuidCollection\" />
            <column name=\"UID\" value=\"$uuidCollection\" />
            <column name=\"OWNERID\" valueComputed=\"SELECT ID FROM USERS WHERE USERNAME='$uname'\" />
            <column name=\"VERSION\" value=\"0\" />
        </insert>
    ";

echo "
        <insert tableName=\"COLLECTION_ITEM\">
            <column name=\"CREATEDATE\" value=\"1371051403601\" />
            <column name=\"ITEMID\" valueComputed=\"SELECT ID FROM ITEM WHERE UID='$uuidCollection' AND ITEMTYPE='collection'\" />
            <column name=\"COLLECTIONID\" valueComputed=\"SELECT ID FROM ITEM WHERE UID='$uuidHomeCollection' AND ITEMTYPE='homecollection'\" />
        </insert>
";

echo "
        <insert tableName=\"ATTRIBUTE\">
            <column name=\"ATTRIBUTETYPE\" value=\"string\" />
            <column name=\"MODIFYDATE\" value=\"1371051403618\" />
            <column name=\"NAMESPACE\" value=\"org.osaf.cosmo.model.CalendarCollectionStamp\" />
            <column name=\"STRINGVALUE\" value=\"user $uname items\" />
            <column name=\"LOCALNAME\" value=\"description\" />
            <column name=\"ITEMID\" valueComputed=\"SELECT ID FROM ITEM WHERE UID='$uuidCollection' AND ITEMTYPE='collection'\" />
        </insert>
";

}

?>    
    </changeSet>

</databaseChangeLog>
