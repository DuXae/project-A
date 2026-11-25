(() => {
    'use strict';

    // NFR-2: все записи одним запросом в пределах серверного лимита
    const API_LIST = '/api/v1/products?page=0&size=10000';
    const COLUMNS = 4;

    const rowsBody = document.getElementById('rows');
    const meta = document.getElementById('meta');
    const footer = document.getElementById('footer');
    const retryBtn = document.getElementById('retry');

    /** FR-8: дата в локальном читаемом формате. */
    function formatDate(isoString) {
        if (!isoString) {
            return '—';
        }
        const date = new Date(isoString);
        if (Number.isNaN(date.getTime())) {
            return '—';
        }
        return date.toLocaleString('ru-RU', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
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

    /** NFR-3: собираем строки во фрагменте, в DOM вставляем один раз. */
    function renderRows(items) {
        const frag = document.createDocumentFragment();

        items.forEach((item) => {
            const tr = document.createElement('tr');

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

            tr.append(idTd, nameTd, qtyTd, dateTd);
            frag.appendChild(tr);
        });

        rowsBody.replaceChildren(frag);
    }

    function setMeta(text) {
        meta.textContent = text;
    }

    async function load() {
        retryBtn.disabled = true;
        footer.hidden = true;
        setMeta('Загрузка…');
        renderMessage('Загрузка…');

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

            // FR-4: общее количество из ответа API
            setMeta(items.length === 1
                ? 'Всего: 1 запись'
                : `Всего: ${page.totalItems != null ? page.totalItems : items.length} записей`);

            if (items.length === 0) {
                // FR-5: пустое состояние
                renderMessage('Товаров пока нет. Добавьте их через форму ввода.', 'empty empty--action');
                return;
            }

            renderRows(items);
        } catch (error) {
            showError('Не удалось связаться с сервером. Проверьте подключение.');
            console.error(error);
        } finally {
            retryBtn.disabled = false;
        }
    }

    /** FR-6: состояние ошибки с кнопкой повтора. */
    function showError(message) {
        setMeta('Ошибка загрузки');
        renderMessage(message, 'empty empty--error');
        footer.hidden = false;
    }

    retryBtn.addEventListener('click', load);

    load();
})();
