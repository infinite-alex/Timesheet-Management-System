(function () {
    var tabs = document.querySelectorAll('[data-tab]');
    var panels = document.querySelectorAll('[data-panel]');

    function activate(name) {
        var found = false;
        tabs.forEach(function (t) {
            var on = t.dataset.tab === name;
            t.classList.toggle('active', on);
            if (on) found = true;
        });
        if (!found) return;
        panels.forEach(function (p) { p.classList.toggle('active', p.dataset.panel === name); });
        document.querySelectorAll('[data-show-on]').forEach(function (el) { el.hidden = el.dataset.showOn !== name; });
        history.replaceState(null, '', '#' + name);
    }

    tabs.forEach(function (t) {
        t.addEventListener('click', function () { activate(t.dataset.tab); });
    });

    document.querySelectorAll('[data-goto]').forEach(function (b) {
        b.addEventListener('click', function () { activate(b.dataset.goto); });
    });

    if (location.hash) activate(location.hash.slice(1));

    document.querySelectorAll('[data-filter]').forEach(function (input) {
        var table = document.querySelector(input.dataset.filter);
        input.addEventListener('input', function () {
            var q = input.value.trim().toLowerCase();
            table.querySelectorAll('tbody tr').forEach(function (tr) {
                tr.hidden = q !== '' && tr.textContent.toLowerCase().indexOf(q) === -1;
            });
        });
    });

    window.toast = function (message, type) {
        var old = document.querySelector('.toast');
        if (old) old.remove();
        var el = document.createElement('div');
        el.className = 'toast' + (type === 'error' ? ' error' : '');
        el.textContent = message;
        document.body.appendChild(el);
        setTimeout(function () { el.remove(); }, 4200);
    };

    window.sendJson = function (method, url, payload) {
        var options = { method: method, headers: {} };
        if (payload !== undefined) {
            options.headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify(payload);
        }
        return fetch(url, options).then(function (res) {
            if (res.redirected || res.status === 401) throw new Error('Sesiunea a expirat. Autentifică-te din nou.');
            if (res.status === 204) return null;
            if (res.ok) return res.json();
            return res.json().catch(function () { return {}; }).then(function (body) {
                var msg = body.error || (res.status === 403
                    ? 'Nu ai permisiunea pentru această acțiune.'
                    : 'Nu s-a putut salva. Verifică datele și încearcă din nou.');
                throw new Error(msg);
            });
        });
    };

    window.postJson = function (url, payload) {
        return window.sendJson('POST', url, payload);
    };

    document.addEventListener('click', function (event) {
        var close = event.target.closest('[data-close]');
        if (close) {
            var dialog = close.closest('dialog');
            if (dialog) dialog.close();
            return;
        }
        if (event.target.tagName === 'DIALOG') event.target.close();
    });

    window.durationLabel = function (minutes) {
        return Math.floor(minutes / 60) + 'h ' + (minutes % 60) + 'm';
    };

    window.dateLabel = function (iso) {
        var p = iso.split('-');
        return p[2] + '.' + p[1] + '.' + p[0];
    };

    window.submitForm = function (form, button, build, url, onDone) {
        form.addEventListener('submit', function (event) {
            event.preventDefault();
            var payload = build();
            if (!payload) return;
            button.disabled = true;
            window.postJson(url, payload).then(function () {
                onDone();
            }).catch(function (err) {
                window.toast(err.message, 'error');
                button.disabled = false;
            });
        });
    };
})();
