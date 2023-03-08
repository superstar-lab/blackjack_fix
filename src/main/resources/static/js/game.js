var ws = null;
var playerId = null;

function setConnected(connected) {
    document.getElementById('connect').disabled = connected;
    document.getElementById('disconnect').disabled = !connected;
}

function setGameOptionsEnabled(enabled) {
    document.getElementById('stay').disabled = !enabled;
    document.getElementById('hit').disabled = !enabled;
    //document.getElementById('split').disabled = !enabled;
}

function setAdmin(enabled) {
    document.getElementById('open').disabled = !enabled;
    document.getElementById('shutdown').disabled = !enabled;
    document.getElementById('numberPlayers').disabled = !enabled;
}

function enableStart(enabled) {
    document.getElementById('start').disabled = !enabled;
}

function setUID(uid) {
    if (uid != null) {
        var stripped = uid.replace(/\./g, ' ');
        document.getElementById('consoleText').innerHTML = 'Console (UID: ' + stripped + ')';
        playerId = uid;
    } else {
        document.getElementById('consoleText').innerHTML = 'Console';
        playerId = '';
    }
}
/**
 * Connect to the server.
 */
function connect() {
    // hardcoded endpoint, oh no!
    ws = new SockJS('/game');
    ws.onopen = function () {
        setConnected(true);
        clientLog('Connection opened.');
    };
    ws.onmessage = function (event) {
        dispatch(event.data);
    };
    ws.onclose = function () {
        setUID();
        clientLog('Connection closed.');
        disconnect();
    };
}

/**
 * Disconnect from the server.
 */
function disconnect() {

    // Before we disconnect let them know we're leaving if its our turn
    if (document.getElementById('stay').disabled === false) {
        ws.send('LEAVING');
    }

    if (ws != null) {
        ws.close();
        ws = null;
    }
    setConnected(false);
    setGameOptionsEnabled(false);
    resetYourText();
    resetDealerText();
    resetOtherText();
    setAdmin(false);
    enableStart(false);
    removeCards();
}

/**
 * Determine what to do with the message.
 *
 * @param message the message.
 */
function dispatch(message) {
    // split message into three: [SENDER, KEY, PAYLOAD]
    var split = message.split('|');
    var logMessage = split[0].concat(split[2]);
    console.log(split);
    switch (split[1]) {
        case 'OTHER+DISCONNECTED':
        case 'OTHER+READY+TO+START':
        case 'OTHER+CONNECTED':
        case 'CONNECTED':
            log(logMessage);
            var connectedMessage = logMessage.split(' ');
            var last = connectedMessage[connectedMessage.length - 1];
            if (split[1] === 'CONNECTED') {
                setUID(last);
            }
            break;
        case 'NOT+ACCEPTING':
            log(logMessage);
            //disconnect(); for now done by the server...this is the work around
            break;
        case 'ADMIN':
            log(logMessage);
            setAdmin(true);
            enableStart(false);
            break;
        case 'GAME+START':
            log(logMessage);
            break;
        case 'OTHER+MOVE':
            log(logMessage);
            break;
        case 'BUST':
            log(logMessage);
            break;
        case 'SKIP':
            log(logMessage);
            break;
        case 'ADD+PLAYER+CARD':
            addCardForPlayer(split[2]);
            break;
        case 'ADD+DEALER+CARD':
            addCardForDealer(split[2]);
            break;
        case 'ADD+OTHER+PLAYER+CARD':
            addCardForOther(split[2], split[3], split[4]);
            break;
        case 'PLAYER+VALUE':
            updatePlayerValue(split[2]);
            break;
        case 'DEALER+VALUE':
            updateDealerValue(split[2]);
            break;
        case 'OTHER+VALUE':
            updateOtherValue(split[2], split[3]);
            break;
        case 'READY+TO+START':
            log(logMessage);
            enableStart(true);
            break;
        case 'DEALING+CARDS':
            removeCards();
            //log(logMessage);
            break;
        case 'YOUR+TURN':
            setGameOptionsEnabled(true);
            log(logMessage);
            break;
        case 'AI+TURN':
            log(logMessage);
            break;
        case 'WINNER':
            log(logMessage);
            break;
        case 'LOSER':
            log(logMessage);
            break;
        case 'RESET':
            log(logMessage);
            setGameOptionsEnabled(false);
            break;
        case 'RESET+ADMIN':
            log(logMessage);
            setGameOptionsEnabled(false);
            enableStart(true);
            break;
        case 'ALL+QUIT':
            log(logMessage);
            setGameOptionsEnabled(false);
            setAdmin(false);
            enableStart(false);
            break;
        default:
            console.log('Unknown message received');
            break;
    }
}

/**
 * Send option chosen back.
 */
function game_option(option) {
    ws.send('GAME_'.concat(option));
    console.log('Sent ' + option);
    clientLog('You decided to ' + option + '. Sending to server - please wait for your next turn.');
    setGameOptionsEnabled(false);
}

/**
 * Send the start message.
 */
function start() {
    ws.send('START_GAME');
    enableStart(false);
    removeCards();
}

/**
 * Add a new card to the player's list.
 */
function addCardForPlayer(card) {
    var li = document.createElement('li');
    li.innerHTML = card;
    document.getElementById('playerHandCards').appendChild(li);
}

/**
 * Add a new card to the dealer's list
 */
function addCardForDealer(card) {
    var li = document.createElement('li');
    li.innerHTML = card;
    document.getElementById('dealerHandCards').appendChild(li);
}

/**
 * Add a new card for another player.
 */
function addCardForOther(card, id, sessionID) {
    var li = document.createElement('li');
    li.innerHTML = card;
    console.log('Trying to append to ' + 'otherHandCards'.concat(id));
    document.getElementById('otherHandCards'.concat(id)).appendChild(li);
    document.getElementById('otherHandText'.concat(id)).innerHTML = "Other Player's Hand (" + sessionID + ")";
}

function removeOldValue(old) {
    var split = old.split('~');
    return split[0];
}

function updatePlayerValue(value) {
    var old = removeOldValue(document.getElementById('yourHandText').innerHTML);
    document.getElementById('yourHandText').innerHTML = old.concat(" ~ Value: ".concat(value));
}

function updateDealerValue(value) {
    var old = removeOldValue(document.getElementById('dealerHandText').innerHTML);
    document.getElementById('dealerHandText').innerHTML = old.concat(" ~ Value: ".concat(value));
}

function updateOtherValue(index, value) {
    var old = removeOldValue(document.getElementById('otherHandText'.concat(index)).innerHTML);
    document.getElementById('otherHandText'.concat(index)).innerHTML = old.concat(" ~ Value: ".concat(value));
}

function resetYourText() {
    document.getElementById('yourHandText').innerHTML = "Your Hand";
}

function resetDealerText() {
    document.getElementById('dealerHandText').innerHTML = "Dealer's Hand";
}

function resetOtherText() {
    document.getElementById('otherHandText1').innerHTML = "Other Player's Hand";
    document.getElementById('otherHandText2').innerHTML = "Other Player's Hand";
}

/**
 * Remove all cards.
 */
function removeCards() {
    console.log('Emptied cards.');
    document.getElementById('playerHandCards').innerHTML = "";
    document.getElementById('dealerHandCards').innerHTML = "";
    document.getElementById('otherHandCards1').innerHTML = "";
    document.getElementById('otherHandCards2').innerHTML = "";
}

/**
 * Open connections for other players.
 */
function acceptOthers() {
    if (ws != null) {
        var numP = document.getElementById('numberPlayers').value;

        clientLog('Opening the lobby with specified settings. When the correct number of players have connected, the start button will become available.');
        var send = 'ACCEPT|' + numP;
        ws.send(send);
        document.getElementById('open').disabled = true;
        document.getElementById('numberPlayers').disabled = true;
    } else {
        alert('Connection not established, please connect.');
    }
}

/**
 * Log from the client.
 * @param message the message.
 */
function clientLog(message) {
    var pad = '00';
    var date = new Date();
    var hour = "" + date.getHours();
    var hourPad = pad.substring(0, pad.length - hour.length) + hour;
    var min = "" + date.getMinutes();
    var minPad = pad.substring(0, pad.length - min.length) + min;
    var hourMin = hourPad + ':' + minPad;
    var prefix = '<strong>' + hourMin + ' Client' + '</strong>: ';
    log(prefix + message);
}

/**
 * Log to the console
 * @param message the message.
 */
function log(message) {
    var console = document.getElementById('console');
    var p = document.createElement('p');
    p.style.wordWrap = 'break-word';
    p.innerHTML = message;

    console.appendChild(p);
    while (console.childNodes.length > 25) {
        console.removeChild(console.firstChild);
    }
    console.scrollTop = console.scrollHeight;
}

/**
 * Shutdown the spring boot server.
 */
function shutdown() {
    clientLog('Sent shutdown command.');
    $.post(
        "http://localhost:8080/shutdown",
        null,
        function () {
            alert("Website shutting down.");
        }
    );
}
