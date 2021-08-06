# Chromis POS DE

Abgeleited von ChromisPos (ChromisPos/Chromis) ab Version 0.95.4

ChromisPosDE soll an die deutsche Gesetzgebung angepasst werden.
Eingebaut wird eine TSE-Schnittstelle, sowie eine digitale Schnittstelle für Kassensysteme (DSFinV-K)




# Chromis Pos DE 0.96.1

Verkäufe – Belegaufteilung

Belege splitten ist jetzt möglich. Die Abhängigkeiten werden im Export DSFinV-K entsprechend abgebildet.

Kassenabschluss

Für jede Münz- und Schein-Art gibt es jetzt zwei Felder. Je eines für abgepackte / vorgezählte Münzen / Scheine. Diese werden für den nächsten Kassenabschluss vorgetragen, damit sie nicht jedes mal hinzugerechnet werden müssen. Das soll eine Fehlerquelle beseitigen.

# Chromis Pos DE 0.96.0

TSE

Die technische Sicherungseinrichtung (TSE) der Bundesdruckerei / Cryptovision wird unterstützt.
Alle notwendigen Daten sowie der optionale QR-Code können auf den Kassenbon gedruckt werden.
Die TSE wird über die Konfirguration „TSE“ der Kasse bekannt gegeben. 
In der Kasse über „Einstellungen – TSE“ kann die TSE für die Kasse eingerichtet werden. Die Berechtigung „Maintenance – TSE initialize“ ist dafür erforderlich. In dieser Maske können offene Transaktionen abgerufen und beendet werden, sowie die PIN´s der TSE neu gesetzt werden.
Weiterhin kann eine automatische Sicherung der TSE-Daten zum Programm-Ende eingerichtet werden, ebenso wie TSE-Daten gesichert und gelöscht werden können.
Damit die TSE-Daten korrekt erstellt werden können, wurde die Pflegemaske der Steuer-Kategorien um die Eigenschaft „TSE-Kategorie“ erweitert.
Bevor korrekte TSE-Daten erstellt werden können müssen die Steuer-Kategorien zuvor entsprechend gepflegt werden.

DSFinV-K

Für die Kassenprüfung durch das Finanzamt ist eine einheitliche Schnittstelle für den Export der Kassen- und TSE-Daten vorhanden. Damit diese nach DSFinV-K funktioniert muss die notwendige Konfiguration als Ressource „Export.DSFinV-K“ hinterlegt sein.
Der Export ist über „Einstellungen – TSE Export“ aufzurufen. Es ist dafür die Berechtigung „Maintenance – TSE Export“ erforderlich.

Round Ticketline TotalValue

Bei Verkäufen mit Mengen mit Nachkommastellen können Rundungsdifferenzen im Kassenbeleg entstehen, die in der Summierung der Bonzeilen mit der Bonsumme offensichtlich werden.
Um das zu vermeiden kann die Summe pro Bonzeile kaufmännisch auf 2 Nachkommastellen gerundet werden. Aktiviert werden kann das Feature über die Konfiguration – System Option – Round Ticketline TotalValue.

Dezimaltrennzeichen

Bei der Eingabe von Zahlen mit Nachkommastellen wird, neben dem Punkt nun auch das Komma akzeptiert. Damit ist die Eingabe mit deutscher Tastatur problemlos auch über den 10-er Block möglich.

Kundenzahlungen

Kundenzahlungen konnten bisher nur bei offenen Forderungen und nur in dieser Höhe angenommen werden.
Nun können Kunden auch Guthaben aufbauen und mit diesem dann bezahlen. Bei Einzahlung von Guthaben kann z.B. ein Kassenbon als Gutschein / Guthaben gedruckt werden.
Zum Drucken von Guthaben wurde die Funktion CustomerInfoExt.printCurDebtXMinus() eingeführt.
Bei Kundenzahlungen können ebenfalls TSE-Daten gedruckt werden.

Einheitliche Belegnummern

Bisher wurden Belegnummern getrennt nach Ticket, Invoice, Payment und Refund fortlaufend erstellt. Diese Differenzierung entspricht nicht der Forderung nach einer „einheitlichen fortlaufenden Belegnummer“. Die Differenzierung wurde aufgehoben und es gibt nur noch eine fortlaufende Nummer für alle Belegarten.

Kassenabschluss

Der Kassenabschluss wurde überarbeitet und kennt nun Anfangs- und Endbestand. Der Kassenbestand wird fortgeschrieben. Ebenso wird nun beim Kassenabschluss eine Bargeldzählung vorgenommen. Kassendifferenzen können dabei ausgebucht werden.
Auf dem Kassenabschlussbericht (Z-Bon) kann die Bargeldzählung gedruckt werden.

Ein-/ Auszahlungen

Für die TSE- und DSFinV-K-Konformität wurde der Dialog Ein-/Auszahlungen um das Feld „Geschäftsvorfall (*)“ erweitert. Bei Ein- und Auszahlungen muss nun der passende Geschäftsvorfall ausgewählt werden. Dieser ist abhängig vom „Anlass“.

Kundendisplay

Die Ausgabe für das Kundendisplay wurde um die Ticket-Informationen erweitert. Es ist jetzt beispielsweise möglich während der Eingabe der zu verkaufenden Artikel die aktuelle Gesamtsumme des Kassenbons auf dem Kundendisplay auszugeben.

Bonjournal

Über das Bonjournal können bereits abgeschlossene Tickets, die noch nicht von einem Tagesabschluss eingeschlossen wurden nun nicht mehr nachbearbeitet werden. Für die Gesetzeskonformität muss der Bon ganz oder teilweise storniert und neu gebucht werden.

Steuer Kategorien

Erweiterung des Dialoges, um die Steuerkategorien mit den entsprechenden TSE-Kategorien zu verknüpfen. Das ist für die Schnittstelle nach DSFinV-K notwenig.
