/** Messages in the shape stored by Mongster and returned by the Mail model. */
exports.sampleMessages = function() {
  return [
    {
      _id: '1', date: 'Sat, 7 Nov 2026 12:00:00 -0700', subject: 'first',
      to: ['anyone@anywhere.com'], cc: [], from: 'someone@somewhere.com', body: 'body one'
    },
    {
      _id: '2', date: 'Sat, 7 Nov 2026 12:05:00 -0700', subject: 'second <b>',
      to: ['a@x.com', 'b@x.com'], cc: ['c@x.com'], from: 'other@somewhere.com', body: 'body two'
    }
  ];
};

/** In-memory stand-ins for the Mail model and the Mongster SMTP server. */
exports.fakeBackend = function(messages) {
  const state = { messages: messages, clears: 0 };
  return {
    state: state,
    Mail: { find: async function() { return state.messages; } },
    smtpServer: { clear: function() { state.clears++; state.messages = []; } }
  };
};
