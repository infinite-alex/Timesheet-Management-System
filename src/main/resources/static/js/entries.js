(function () {
    var editDialog = document.getElementById('editDialog');
    var deleteDialog = document.getElementById('deleteDialog');
    if (!editDialog || !deleteDialog) return;

    var editForm = document.getElementById('editForm');
    var saveButton = document.getElementById('editSave');
    var confirmButton = document.getElementById('deleteConfirm');
    var current = null;

    function readRow(button) {
        var row = button.closest('tr');
        return {
            id: row.dataset.id,
            date: row.dataset.date,
            who: row.dataset.who || '',
            client: row.dataset.client || '',
            clientName: row.dataset.clientName || 'fără client',
            task: row.dataset.task || '',
            month: row.dataset.month,
            minutes: Number(row.dataset.minutes),
            note: row.dataset.note || ''
        };
    }

    document.addEventListener('click', function (event) {
        var edit = event.target.closest('[data-edit]');
        var del = event.target.closest('[data-delete]');
        if (!edit && !del) return;
        current = readRow(edit || del);

        if (edit) {
            document.getElementById('editSubtitle').textContent =
                (current.who ? current.who + ' · ' : '') + window.dateLabel(current.date);
            document.getElementById('editMonth').value = current.month;
            document.getElementById('editClient').value = current.client;
            document.getElementById('editTask').value = current.task;
            document.getElementById('editHours').value = Math.floor(current.minutes / 60);
            document.getElementById('editMinutes').value = current.minutes % 60;
            document.getElementById('editNote').value = current.note;
            saveButton.disabled = false;
            editDialog.showModal();
        } else {
            document.getElementById('deleteText').textContent =
                window.dateLabel(current.date) + ' · ' + window.durationLabel(current.minutes) + ' · ' + current.clientName
                + (current.who ? ' (' + current.who + ')' : '');
            confirmButton.disabled = false;
            deleteDialog.showModal();
        }
    });

    editForm.addEventListener('submit', function (event) {
        event.preventDefault();
        var total = Number(document.getElementById('editHours').value || 0) * 60
            + Number(document.getElementById('editMinutes').value || 0);
        if (total <= 0) {
            window.toast('Introdu o durată mai mare de zero.', 'error');
            return;
        }
        var client = document.getElementById('editClient').value;
        var payload = {
            date: current.date,
            employeeId: null,
            clientId: client ? Number(client) : null,
            workingmonth: document.getElementById('editMonth').value,
            totalMinutes: total,
            actions: [document.getElementById('editTask').value],
            extranote: document.getElementById('editNote').value
        };
        saveButton.disabled = true;
        window.sendJson('PUT', '/api/timesheet-entries/' + current.id, payload)
            .then(function () { location.reload(); })
            .catch(function (err) {
                window.toast(err.message, 'error');
                saveButton.disabled = false;
            });
    });

    confirmButton.addEventListener('click', function () {
        confirmButton.disabled = true;
        window.sendJson('DELETE', '/api/timesheet-entries/' + current.id)
            .then(function () { location.reload(); })
            .catch(function (err) {
                deleteDialog.close();
                window.toast(err.message, 'error');
                confirmButton.disabled = false;
            });
    });
})();
