(() => {
    'use strict';

    const API_LIST = '/api/v1/products?page=0&size=10000';
    const API_DELETE = (id) => `/api/v1/products/${id}`;
    const API_BULK_DELETE = '/api/v1/products/bulk-delete';
    const COLUMNS = 6;

    const rowsBody = document.getElementById('rows');
    const meta = document.getElementById('meta');
    const statusEl = document.getElementById('status');
    const selectAll = document.getElementById('select-all');
    const deleteSelectedBtn = document.getElementById('delete-selected');
    const modal = document.getElementById('confirm-modal');
    const confirmList = document.getElementById('confirm-list');
    const confirmOk = document.getElementById('confirm-ok');
    const confirmCancel = document.getElementById('confirm-cancel');

    /** Текущий список записей: id → товар (для текста подтверждения). */
    let currentItems = new Map();
    /** Ожидающее подтверждения действие: { ids: number[] }. */
    let pending = null;

    function formatDate(isoString) {
        if (!isoString) {
            return '—';
        }
        const date = new Date(isoString);
        if (Number.isNaN(date.getTime())) {
            return '—';
        }
        return date.toLocaleString('ru-RU', {
            day: '2-digit', month: '2-digit', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    }

    function setStatus(message, kind) {
        statusEl.textContent = message || '';
        statusEl.className = 'status' + (kind ? ` status--${kind}` : '');
    }

    function setMeta(text) {
        meta.textContent = text;
    }

    function renderMessage(text, className) {
        rowsBody.replaceChildren();
        const tr = document.createElement('tr');
        const td = document.createElement('td');
        td.colSpan = COLUMNS;
        td.className = className || 'empty';
        td.textContent = text;

        if (className === 'empty empty--action') {
            const link = document.createElement('a');
            link.href = '/';
            link.textContent = 'Перейти к форме ввода';
            td.append(document.createElement('br'), link);
        }

        tr.appendChild(td);
        rowsBody.appendChild(tr);
    }

    function selectedIds() {
        return [...rowsBody.querySelectorAll('input.row-check:checked')]
            .map((input) => Number(input.value));
    }

    function updateSelectionState() {
        const ids = selectedIds();
        deleteSelectedBtn.disabled = ids.length === 0;
        deleteSelectedBtn.textContent = ids.length > 0
            ? `Удалить выбранные (${ids.length})`
            : 'Удалить выбранные';

        const boxes = rowsBody.querySelectorAll('input.row-check');
        selectAll.checked = boxes.length > 0 && ids.length === boxes.length;
        selectAll.indeterminate = ids.length > 0 && ids.length < boxes.length;
    }

    function renderRows(items) {
        const frag = document.createDocumentFragment();

        items.forEach((item) => {
            const tr = document.createElement('tr');

            const checkTd = document.createElement('td');
            checkTd.className = 'col-check';
            const check = document.createElement('input');
            check.type = 'checkbox';
            check.className = 'row-check';
            check.value = String(item.id);
            check.setAttribute('aria-label', `Выбрать «${item.name}»`);
            checkTd.appendChild(check);

            const idTd = document.createElement('td');
            idTd.className = 'col-id';
            idTd.textContent = String(item.id);

            const nameTd = document.createElement('td');
            nameTd.textContent = item.name;

            const qtyTd = document.createElement('td');
            qtyTd.className = 'col-qty';
            qtyTd.textContent = String(item.quantity);

            const dateTd = document.createElement('td');
            dateTd.className = 'col-date';
            dateTd.textContent = formatDate(item.createdAt);

            const actionTd = document.createElement('td');
            actionTd.className = 'col-action';
            const deleteBtn = document.createElement('button');
            deleteBtn.type = 'button';
            deleteBtn.className = 'btn btn--danger btn--small';
            deleteBtn.textContent = 'Удалить';
            deleteBtn.setAttribute('aria-label', `Удалить «${item.name}»`);
            deleteBtn.addEventListener('click', () => askConfirm([item.id]));
            actionTd.appendChild(deleteBtn);

            tr.append(checkTd, idTd, nameTd, qtyTd, dateTd, actionTd);
            frag.appendChild(tr);
        });

        rowsBody.replaceChildren(frag);
        updateSelectionState();
    }

    async function load() {
        setMeta('Загрузка…');
        renderMessage('Загрузка…');
        selectAll.checked = false;
        selectAll.indeterminate = false;
        deleteSelectedBtn.disabled = true;

        try {
            const response = await fetch(API_LIST, { headers: { Accept: 'application/json' } });

            if (!response.ok) {
                const payload = await response.json().catch(() => null);
                const details = payload && payload.message ? `: ${payload.message}` : '';
                showError(`Не удалось загрузить список (HTTP ${response.status})${details}`);
                return;
            }

            const page = await response.json();
            const items = Array.isArray(page.items) ? page.items : [];

            currentItems = new Map(items.map((item) => [item.id, item]));
            setMeta(items.length === 1 ? 'Всего: 1 запись' : `Всего: ${items.length} записей`);

            if (items.length === 0) {
                renderMessage('Нечего удалять — товаров нет.', 'empty empty--action');
                return;
            }

            renderRows(items);
        } catch (error) {
            showError('Не удалось связаться с сервером. Проверьте подключение.');
            console.error(error);
        }
    }

    function showError(message) {
        setMeta('Ошибка загрузки');
        renderMessage(message, 'empty empty--error');
        setStatus('Повторите попытку позже.', 'error');
    }

    /** FR-5: показываем диалог с перечнем удаляемых товаров. */
    function askConfirm(ids) {
        if (ids.length === 0) {
            return;
        }
        pending = { ids };

        confirmList.replaceChildren();
        ids.forEach((id) => {
            const item = currentItems.get(id);
            const li = document.createElement('li');
            li.textContent = item ? `${item.name} — ${item.quantity}` : `ID ${id}`;
            confirmList.appendChild(li);
        });

        modal.hidden = false;
        confirmOk.focus();
    }

    function closeConfirm() {
        modal.hidden = true;
        pending = null;
    }

    async function performDelete(ids) {
        const single = ids.length === 1;
        confirmOk.disabled = true;
        setStatus('Удаление…');

        try {
            const response = single
                ? await fetch(API_DELETE(ids[0]), { method: 'DELETE' })
                : await fetch(API_BULK_DELETE, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json; charset=UTF-8', Accept: 'application/json' },
                    body: JSON.stringify({ ids })
                });

            if (!response.ok) {
                const payload = await response.json().catch(() => null);

                // FR-10: запись уже удалена в другой вкладке
                if (response.status === 404) {
                    await load();
                    setStatus('Запись уже удалена. Список обновлён.', 'error');
                    return;
                }

                const details = payload && payload.details && payload.details.length > 0
                    ? `: ${payload.details.join('; ')}`
                    : '';
                setStatus(`Не удалось удалить (HTTP ${response.status})${details}`, 'error');
                return;
            }

            const count = single ? 1 : (await response.json().catch(() => ({ deleted: ids.length }))).deleted;
            await load();
            setStatus(
                count === 1 ? 'Удалено записей: 1' : `Удалено записей: ${count}`,
                'success'
            );
        } catch (error) {
            setStatus('Не удалось связаться с сервером. Проверьте подключение.', 'error');
            console.error(error);
        } finally {
            confirmOk.disabled = false;
        }
    }

    // Выбор строк
    rowsBody.addEventListener('change', (event) => {
        if (event.target.classList.contains('row-check')) {
            updateSelectionState();
        }
    });

    selectAll.addEventListener('change', () => {
        const checked = selectAll.checked;
        rowsBody.querySelectorAll('input.row-check').forEach((input) => {
            input.checked = checked;
        });
        updateSelectionState();
    });

    deleteSelectedBtn.addEventListener('click', () => askConfirm(selectedIds()));

    // Диалог
    confirmOk.addEventListener('click', () => {
        if (!pending) {
            return;
        }
        const { ids } = pending;
        closeConfirm();
        performDelete(ids);
    });

    confirmCancel.addEventListener('click', closeConfirm);
    modal.addEventListener('click', (event) => {
        if (event.target.dataset.close === 'true') {
            closeConfirm();
        }
    });
    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && !modal.hidden) {
            closeConfirm();
        }
    });

    load();
})();
